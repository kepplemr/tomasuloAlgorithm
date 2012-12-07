package functionalUnits;
import java.util.ArrayDeque;
import simulator.*;

/** 
 * @author Michael Kepple
 * @version November 30th, 2012
 */
public class MemUnit extends FunctionalUnit
{
	ArrayDeque<Station> executionQueue;
	ArrayDeque<Station> writeQueue;
	int loadIndex;
	int storeIndex;
	
	/** MemUnit constructor - Loads and stores are handles by the same memory unit and are executed in the order in which they
	 *   are issued.
	*/
	public MemUnit()
	{
		RScount = 8;
		loadIndex = 0;
		storeIndex = 4;
	    executionCount = 11;
	    RS = new Station[RScount];
	    executionQueue = new ArrayDeque<Station>();
	    writeQueue = new ArrayDeque<Station>();
	    for (int i = 0; i < storeIndex; i++)
	    	RS[i] = new Station("Load"+i);
	    for (int i = storeIndex; i < RScount; i++)
	    	RS[i] = new Station("Store"+(i-4));
	}
	
	/** insertInstruction - inserts a load or store instruction into appropriate reservation stations. Also
	 *    updates the execution order to make sure the loads and stores get executed in order.
	 * @param operation - operation to be performed
	 * @param rs - base register for address calculation. Always a GPR
	 * @param rt - register to be stored or loaded. Can be general register or floating point.
	 * @param imm - immediate offset value to be added w/ rs. 
	 * @return true if operation was inserted, false if stall required.
	 */
	public boolean insertInstruction(String operation, int rs, int rt, int imm)
	{
		GPR generalRegs = GPR.getInstance();
		FPR floatingRegs = FPR.getInstance();
		if ((operation.equals("l.d")) || (operation.equals("ld")))
		    for (int i = 0; i < storeIndex; i++)
		    	if (!RS[i].busy)
		    	{
		    		RS[i].busy = true;
		    		RS[i].operation = operation;
		    		RS[i].A = imm;	
		    		if (generalRegs.isFree(rs))
		    			RS[i].Vj = generalRegs.getRegister(rs);
		    		else
		    			RS[i].Qj = generalRegs.getResStation(rs);
		    		if (operation.equals("l.d"))
		    			floatingRegs.setResStation(rt, "Load"+i);
		    		else
		    			generalRegs.setResStation(rt, "Load"+i);
		    		StatusTable.getInstance().addInstruction(operation + ((operation.equals("ld")) ? " r" : " f") + rt + " " + imm + " r" + rs, RS[i].name);
		    		executionQueue.add(RS[i]);
		    		return false;
		      }
		if ((operation.equals("s.d")) || (operation.equals("sd"))) 
			for (int i = storeIndex; i < RScount; i++)
				if (!RS[i].busy)
	            {
					RS[i].busy = true;
					RS[i].operation = operation;
					RS[i].A = imm;
					if (generalRegs.isFree(rs))
						RS[i].Vj = generalRegs.getRegister(rs);
					else
						RS[i].Qj = generalRegs.getResStation(rs);
					Registers storing = ((operation.equals("s.d") ? floatingRegs : generalRegs));
					if (storing.isFree(rt))
						RS[i].Vk = storing.getRegister(rt);
					else
						RS[i].Qk = storing.getResStation(rt);
					executionQueue.add(RS[i]);
					StatusTable.getInstance().addInstruction(operation + ((operation.equals("sd")) ? " r" : " f") + rt + " " + imm + " r" + rs, RS[i].name);
					return false;
	            }
		return true;
	}
	
	/** get NextInstr - according to issue order, returns the next Station ready to be executed or null if none
	 *    are currently ready.
	 * @return - next station, or null.
	 */
	Station getNextInstr()
	{
		if (executionQueue.size() == 0)
			return null;
		Station next = executionQueue.peek();
		if (next.ready())
			return next;
		return null;
	}
	
	/** execute - if the functional unit is available and a valid instruction is waiting in the
	 *    executionQueue, begin execution. If an instruction finishes, mark the functional unit not busy
	 *    and add result to the queue to be written.
	 */
	public boolean execute()
	{
		Station execute;
		if (!FUbusy)
		{
			execute = getNextInstr();
			if (execute != null)
			{
				FUbusy = true;
				// Calculate base + offset
				execute.A = execute.Vj + execute.A;
				StatusTable.getInstance().updateStartEX(execute.name);
				executionCycles = (executionCount-1);
			}
		}
		else
		{
			executionCycles--;
			execute = executionQueue.peek();
			if (executionCycles == 0)
			{
				FUbusy = false;
				execute.resultReady = true;
				executionQueue.remove();
				writeQueue.add(execute);
				StatusTable.getInstance().updateEndEX(execute.name);
			}
		}
		return false;
	}
	
	/** write - write stage for loads/stores. Since a store does not write to the CDB, both a store write
	 *    and a load write (which does write to CDB) can occur during the same cycle.
	 *  @return - common data bus representation.
	 */
	public CDB write()
	{
		CDB ret = null;
		boolean loadWritten = false;
		boolean storeWritten = false;
		for (int i = 0; i < 2; i++)
		{
			if (writeQueue.isEmpty())
				break;
			Station write = writeQueue.peek();
			if ((write.operation.equals("sd") || write.operation.equals("s.d")) && !storeWritten)
			{
				write = writeQueue.remove();
				write.resultWritten = true;
			    StatusTable.getInstance().updateWrite(write.name);
				Memory.getInstance().putLong(write.A, write.Vk);
				
				storeWritten = true;
			}
			else if ((write.operation.equals("ld") || write.operation.equals("l.d")) && !loadWritten)
			{
				write = writeQueue.remove();
				write.resultWritten = true;
			    StatusTable.getInstance().updateWrite(write.name);
				ret = new CDB();
		        ret.result = Memory.getInstance().getLong(write.A);
		        ret.station = write.name;
		        loadWritten = true;
			}
		}
		return ret;
	}
	
	/** dump - prints message specifying which Functional Units is dumping, calls Superclass' dump method.
	 */
	public void dump()
	{	
	    System.out.println("Load Buffers");
	    Station.dumpHeader();
	    for (int i = 0; i < storeIndex; i++) 
	    {
	    	RS[i].dump();
	    }
	    System.out.println("\nStore Buffers");
	    Station.dumpHeader();
	    for (int i = storeIndex; i < RScount; i++)
	    {
	    	RS[i].dump();
	    }
	    System.out.println();
	}

	@Override
	void computeResult(int station) 
	{
		// TODO Auto-generated method stub
		
	}

}

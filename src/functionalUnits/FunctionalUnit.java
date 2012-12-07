package functionalUnits;

import simulator.CDB;
import simulator.Station;
import simulator.StatusTable;

/** 
 * @author Michael Kepple
 * @version November 30th, 2012
 */
public abstract class FunctionalUnit 
{
	Station [] RS; //array of reservation stations that feed functional unit
	int RScount; //number of reservation stations
	int executionCount; //number of execution cycles required by the functional unit
	int currentInstruction; //index into the RS array identifying current instruction being executed
	boolean FUbusy; //flag indicating whether the functional unit is currently executing an instruction
	int executionCycles; //number of execution cycles remaining for currently executing instruction

	/** findInstructionToExecute - looks through the reservation stations for one with a ready value of true 
	 *    and returns the index. Uses ready function from Station class. 
	 *  @return index of reservation station ready. 
	 */
	int findInstructionToExecute()
	{
		for (int i = 0; i < RScount; i++)
	    {
			if (RS[i].ready()) 
				return i;
	    }
		// if no instructions in this FU's reservations stations are ready yet.
	    return -1;
	}
	
	/** execute - if functional unit is available, execute the next available instruction from the 
	 *    reservation stations if one is available. Otherwise, continue executing the current instruction
	 *    and update fields if instruction completes in this cycle.
	 */   
	public boolean execute()
	{
		if (!FUbusy)
	    {
			currentInstruction = findInstructionToExecute();
			// If FU is free & an instruction is ready to go
			if (currentInstruction != -1)
			{
				StatusTable.getInstance().updateStartEX(RS[currentInstruction].name);
				FUbusy = true;
				executionCycles = (executionCount-1);
			}
	    }
	    else 
	    {
	    	executionCycles--;
	    	// If FU just finished executing an instruction
	    	if (executionCycles == 0)
	    	{
	    		FUbusy = false;
	    		RS[currentInstruction].resultReady = true;
	    		computeResult(currentInstruction);
	    		StatusTable.getInstance().updateEndEX(RS[currentInstruction].name);
	    	}
	    }
		return false;
	}
	
	/** updateReservationStations - called from main Simulator loop, updates any reservation stations
	 *    that were waiting on results.
	 *  @param cdb - Common Data Bus
	 */
	public void updateReservationStations(CDB cdb)
	{
		for (int i = 0; i < RScount; i++)
		{
			if (RS[i].busy)
			{
				// Since we now have result, null corresponding Q field and fill V value.
				if (cdb.station.equals(RS[i].Qj))
				{
					RS[i].Qj = null;
					RS[i].Vj = cdb.result;
				}
				else if (cdb.station.equals(RS[i].Qk))
				{
					RS[i].Qk = null;
					RS[i].Vk = cdb.result;
				}
			}
		}
	}
	
	/** findIntructionToWrite - looks for a reservation station with a resultReady of true.
	 *  @return index of reservation station whose result has been calculated or -1 if none are.
	 */
	int findInstructionToWrite()
	{
		for (int i = 0; i < RScount; i++)
	    {
			if (RS[i].resultReady)
				return i;
	    }
	    return -1;
	}
	
	/** write - if an instruction in one of the reservation stations has it's result, create a CDB
	 *    object to send it out and update the status table appropriately.
	 *  @return push - CDB object containing RS's name and the result it produced.
	 */
	public CDB write()
	{
	    int ready = findInstructionToWrite();
	    if (ready != -1)
	    {
	    	// Update Status table
	    	StatusTable.getInstance().updateWrite(RS[ready].name);
	    	// Create new CDB object
	    	CDB push = new CDB();
	    	push.result = RS[ready].result;
	    	push.station = RS[ready].name;
	    	RS[ready].resultWritten = true;
	    	return push;
	    }
	    return null;
	}
	
	/** dump - calls helpful header-creator function in Station then dumps all the contents of reservation
	 *    station associated with the functional unit.
	 */
	public void dump()
	{
		Station.dumpHeader();
		for (Station stat: RS)
			stat.dump();
		System.out.println();
	}
	
	/** clear - if any reservation stations in this functional unit have finished writing their results, boot them
	 *    out to make room for more issuing instructions.
	 */
	public void clear()
	{
		for (int i = 0; i < RScount; i++)
		{
			if (RS[i].resultWritten) 
				RS[i].clear();
		}
	}
	
	/** finished - check if all reservation stations in this functional unit are cleared.
	 * @return true if all have finished, false otherwise.
	 */
	public boolean finished()
	{
		for (int i = 0; i < RScount; i++)
		{
			if (RS[i].busy == true)
				return false;
		}
		return true;
	}
	
	/** computeResult - abstract method to be implemented by the specific functional units.
	 * @param station - reservation station to use.
	 */
	abstract void computeResult(int station);
}

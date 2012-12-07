package functionalUnits;
import simulator.*;

/** 
 * @author Michael Kepple
 * @version November 30th, 2012
 */
public class BranchUnit extends FunctionalUnit
{
	
	/** BranchUnit constructor - sets super class fields
	 */
	public BranchUnit()
	{
	    RScount = 1;
	    executionCount = 9;
	    RS = new Station[RScount];
	    for (int i = 0; i < RScount; i++) 
	    	RS[i] = new Station("Branch" + i);
	    currentInstruction = 0;
	    FUbusy = false;
	}
	
	/** insertInstruction - inserts a branch or jump instruction into appropriate reservation stations.
	 *  @param operation - operation to be performed
	 *  @param rs - first operand to be compared
	 *  @param rt - second operand to be compared
	 *  @param imm - immediate offset value for branch instruction
	 *  @param limm - longer offset value for jump instructions
	 *  @return true if operation was inserted, false if stall required.
	 */
	public boolean insertInstruction(String operation, int rs, int rt, int imm, int limm)
	{
		if (operation.equals("beq") || operation.equals("bne"))
		{
			for (int i = 0; i < RScount; i++)
				if (!RS[i].busy)
				{
					RS[i].busy = true;
					RS[i].operation = operation;
					RS[i].A = Tools.signExtend(16, imm) * 4;
					StatusTable.getInstance().addInstruction(operation + " r" + rt + " r" + rs + " " + RS[i].A, RS[i].name);
					GPR generalRegs = GPR.getInstance();
					if (generalRegs.isFree(rs))
						RS[i].Vj = generalRegs.getRegister(rs);
					else
						RS[i].Qj = generalRegs.getResStation(rs);
					if (generalRegs.isFree(rt))
						RS[i].Vk = generalRegs.getRegister(rt);
					else
						RS[i].Qk = generalRegs.getResStation(rt);
					return false;
				}
		}
		else
		{
			for (int i = 0; i < RScount; i++)
				if (!RS[i].busy)
				{
					StatusTable.getInstance().addInstruction(operation + " " + limm, RS[i].name);
					RS[i].busy = true;
					RS[i].operation = operation;
					RS[i].A = limm * 4;
					return false;
				}
			
		}
		return true;
	}
	
	/** execute - if the functional unit is free and an instruction is available, execute it. 
	 *  @return true if a branch is being executed.
	 */
	public boolean execute()
	{
		if (!FUbusy)
		{
			currentInstruction = findInstructionToExecute();
			if (currentInstruction != -1)
			{
				StatusTable.getInstance().updateStartEX(RS[currentInstruction].name);
				FUbusy = true;
				if (RS[currentInstruction].operation.equals("j"))
					executionCycles = 1;
				else
					executionCycles = executionCount -1;
				return true;
			}
		}
		else
		{
			executionCycles--;
			if (executionCycles == 0)
			{
		        StatusTable.getInstance().updateEndEX(this.RS[this.currentInstruction].name);
		        FUbusy = false;
		        RS[currentInstruction].resultReady = true;
			}
			return true;
		}
		return !finished();
	}
	
	/** write - the branch unit does not write it's results to the CDB, rather it updates the PC
	 *    if the branch of jump is determined to be taken.
	 *  @return null
	 */
	public CDB write()
	{
		int instr = findInstructionToWrite();
		if (instr != -1)
		{
			StatusTable.getInstance().updateWrite(RS[instr].name);
			int offset = (int) RS[instr].A;
			long op1 = RS[instr].Vj;
			long op2 = RS[instr].Vk;
			int address;
			if (RS[instr].operation.equals("beq"))
			{
			      address = Simulator.getPC() + offset;
			      if (op1 == op2)
			      {
			    	  RS[instr].result = -1L; 
			    	  Simulator.setPC(address);
			      }
			}
			else if (RS[instr].operation.equals("bne"))
			{
				address = Simulator.getPC() + offset;
			    if (op1 != op2)
			    {
			    	RS[instr].result = -1L; 
			    	Simulator.setPC(address);
			    }
			}
			else if (RS[instr].operation.equals("j"))
			{
				address = offset;
				RS[instr].result = -1L;
				Simulator.setPC(address);
			}
			RS[instr].resultWritten = true;
		}
		return null;
	}


	@Override
	void computeResult(int station) 
	{
		// TODO Auto-generated method stub
	}
}

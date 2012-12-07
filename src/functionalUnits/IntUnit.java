package functionalUnits;
import simulator.*;

/** 
 * @author Michael Kepple
 * @version November 30th, 2012
 */
public class IntUnit extends FunctionalUnit
{
	
	/** Constructor for Integer Functional Unit. Sets fields and initializes its associated
	 *    reservation stations.
	 */
	public IntUnit()
	{
		executionCount = 7;
		RScount = 4;
		RS = new Station[RScount];
	    for (int i = 0; i < RScount; i++) 
	    	RS[i] = new Station("Integer" + i);
	    currentInstruction = 0;
	    FUbusy = false;
	}
	
	/** computeResult - given the reservation station, fetch operands and perform
	 *    the intended integer computation.
	 *  @param statNum - index of the reservation station holding the op, operands that
	 *    we're to compute.
	 */
	void computeResult(int statNum)
	{
		String operation = RS[statNum].operation;
		long op1 = RS[statNum].Vj;
		long op2 = RS[statNum].Vk;
		long imm = RS[statNum].A;
		long result;
		if (operation.equals("dadd"))
			result = op1 + op2;
		else if (operation.equals("dsub"))
			result = op1 - op2;
		else
			result = op1 + imm;
		RS[statNum].result = result;
	}
	
	/** insertInstruction - inserts instructions into reservation station if spot available; updates status table
	 *  @param operation - name of instruction
	 *  @param dest - destination register for operation
	 *  @param op1 - operand register 1
	 *  @param op2 - operand register 2
	 *  @return - true if successfully added, false otherwise.
	 */
	public boolean insertInstruction(String operation, int dest, int op1, int op2)
	{
		GPR generalRegs = GPR.getInstance();
		for (int i = 0; i < RScount; i++)
		{
			if (!RS[i].busy)
			{
				RS[i].busy = true;
		        RS[i].operation = operation;
				// If operand is ready in register, grab it
				if (generalRegs.isFree(op1))
					RS[i].Vj = generalRegs.getRegister(op1);
				else
					RS[i].Qj = generalRegs.getResStation(op1);
				if (generalRegs.isFree(op2))
					RS[i].Vk = generalRegs.getRegister(op2);
				else
					RS[i].Qk = generalRegs.getResStation(op2);
				// Note that the output of this instruction is headed to dest register
		        generalRegs.setResStation(dest, "Integer" + i);
				// Update status table
				StatusTable.getInstance().addInstruction(operation + " r" + dest + " r" + op1 + " r" + op2, RS[i].name);
				return false;
			}
		}
		return true;
	}
	
	/** insertImmInstr - inserts immediate Integer I-type instruction into reservation station is spot available;
	 *    updates status table. 
	 *  @param operation - name of operation to insert.
	 *  @param rs - operand to use with the immediate value.
	 *  @param rt - destination of operation
	 *  @param imm - immediate value, low order 16 bits contain.
	 *  @return - true if successfully added, false otherwise.
	 */
	public boolean insertImmInstr(String operation, int rs, int rt, int imm)
	{
		GPR generalRegs = GPR.getInstance();
	    for (int i = 0; i < RScount; i++)
	    {
	    	if (!RS[i].busy)
	    	{
	    		// Getting here
	    		RS[i].busy = true;
	    		RS[i].operation = operation;
	    		long ext = imm;
	    		// Sign extend immediate value to long if signed add
	    		// Bit is not zero-based.
	    		if (operation.equals("daddi"))
	    			ext = Tools.signExtend(16, ext);
	    		//System.out.println("Ext: " + ext);
	    		StatusTable.getInstance().addInstruction(operation + " r" + rt + " r" + rs + " " + ext, RS[i].name);
	    		RS[i].A = ext;
	    		// Check if other operand is available
	    		if (generalRegs.isFree(rs))
	    			RS[i].Vj = generalRegs.getRegister(rs);
	    		else
	    			RS[i].Qj = generalRegs.getResStation(rs);
	    		// Must set res stations AFTER checking for register availability
	    		generalRegs.setResStation(rt, ("Integer" + i));
	    		// Update status table
	    		return false;
	    	}
	    }
	    return true;
	}
	
	/** dump - prints message specifying which Functional Units is dumping, calls Superclass' dump method.
	 */
	public void dump()
	{
		System.out.println("Integer Reservation Stations:");
		super.dump();
	}
	
}

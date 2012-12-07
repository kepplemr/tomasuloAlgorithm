package functionalUnits;
import simulator.*;

/** 
 * @author Michael Kepple
 * @version November 30th, 2012
 */
public class FPAdder extends FunctionalUnit
{
	
	/** Constructor for FPAdder Functional Unit. Sets fields and initializes its associated
	 *    reservation stations.
	 */
	public FPAdder()
	{
		executionCount = 13;
		RScount = 4;
		RS = new Station[RScount];
	    for (int i = 0; i < RScount; i++) 
	    	RS[i] = new Station("FPAdd" + i);
	    currentInstruction = 0;
	    FUbusy = false;
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
		// Order of params: rd rs rt
		FPR floatingRegs = FPR.getInstance();
	    for (int i = 0; i < RScount; i++)
	    {
	    	if (!RS[i].busy)
	    	{
	    		StatusTable.getInstance().addInstruction(operation + " f" + dest + " f" + op1 + " f" + op2, RS[i].name);
	    		RS[i].busy = true;
	    		RS[i].operation = operation;
	    		if (floatingRegs.isFree(op1))
	    			RS[i].Vj = floatingRegs.getRegister(op1);
	    		else 
	    			RS[i].Qj = floatingRegs.getResStation(op1);
	    		if (floatingRegs.isFree(op2))
	    			RS[i].Vk = floatingRegs.getRegister(op2);
	    		else 
	    			RS[i].Qk = floatingRegs.getResStation(op2);
	    		floatingRegs.setResStation(dest, "FPAdd" + i);
	    		return false;
	    	}
	    }
	    return true;
	}

	/** computeResult - given the reservation station, fetch operands and perform
	 *    the intended FPAdder computation.
	 *  @param station - index of the reservation station holding the op, operands that
	 *    we're to compute.
	 */
	void computeResult(int station) 
	{
		double op1 = Double.longBitsToDouble(RS[station].Vj);
	    double op2 = Double.longBitsToDouble(RS[station].Vk);
	    long result = 0;
	    if (RS[station].operation.equals("add.d"))
	    	result = Double.doubleToLongBits(op1 + op2);
	    else if (RS[station].operation.equals("sub.d"))
	    	result = Double.doubleToLongBits(op1 - op2);
	    RS[station].result = result;
	}
	
	/** dump - prints message specifying which Functional Units is dumping, calls Superclass' dump method.
	 */
	public void dump()
	{
		System.out.println("FP Adder Reservation Stations");
		super.dump();
	}  
}

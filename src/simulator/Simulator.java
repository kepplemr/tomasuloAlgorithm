package simulator;
import functionalUnits.*;
import java.io.*;
import java.util.*;
import tsgui.*;

/** 
 * 
 * @author Michael Kepple
 * @version November 30th, 2012
 *
 */
public class Simulator
{
	static int PC;
	boolean halt;
	// Functional Units
	IntUnit intUnit;
	FPAdder fadder;
	MemUnit memUnit;
	BranchUnit branchUnit;
	FPDiv fdiv;
	FPMult fmult;
	
    //both of these are for the GUI 
    boolean gui;
    private ArrayList<CycleSnapShot> snapshots = null;

    public Simulator(String file, boolean flag) throws IOException
    {
    	Memory me = Memory.getInstance();
    	me.loadMemory(file);
    	
        //create your functional units in here
    	intUnit = new IntUnit();
    	fadder = new FPAdder();
    	memUnit = new MemUnit();
    	branchUnit = new BranchUnit();
    	fdiv = new FPDiv();
    	fmult = new FPMult();
        gui = flag;
    }
    
    /** simulate - Main Tomasulo simulation loop. Handles Issue, Execute, Write of instructions.
     */
    public void simulate()
    {
        PC = 0;
        int instruction = 0;
        boolean stall = false;
        boolean branch = false;
        while (halt == false || !finished())
        {
        	String station = "";
        	long result = 0;
			CDB cdb = write(); 
            branch = execute();
            if ((!halt) && (!branch))
            {
            	instruction = Memory.getInstance().getWord(PC);
            	// stall set to true if issue fails
            	stall = issue(instruction);
            	if ((!halt) && (!stall))
            		PC += 4;		
            }
            updateReservationStations(cdb);
            clearReservationStations();
        	Clock.getInstance().increment();
            if (cdb != null)
            {
            	station = cdb.station;
            	result = cdb.result;
            }
            if (gui) 
            	addSnapShot(instruction, PC, result, station);
        }
        //if (gui == true) 
        //	new TSGui(snapshots);
        Memory.getInstance().dump();
        GPR.getInstance().dump();
        FPR.getInstance().dump();
        StatusTable.getInstance().dump();
        System.out.println("Total clock cycles: " + Clock.getInstance().get());
    }
    
    
    /** finished - make sure all functional units have finished executing.
     * @return true if all FUs clear, false otherwise.
     */
    public boolean finished()
    {
    	return (memUnit.finished() && fadder.finished() && intUnit.finished());
    }
    
    /** updateReservationStations - according to the CDB, update any reservations stations that were
     *    waiting on that result, and update the registers if no reservation stations needed the value.
     *  @param cdb - Common Data Bus
     */
    public void updateReservationStations(CDB cdb)
    {
    	if (cdb != null)
    	{
    		branchUnit.updateReservationStations(cdb);
    		memUnit.updateReservationStations(cdb);
    		fdiv.updateReservationStations(cdb);
    		fmult.updateReservationStations(cdb);
    		fadder.updateReservationStations(cdb);
    		intUnit.updateReservationStations(cdb);
    		GPR.getInstance().updateRegisterFile(cdb);
    		FPR.getInstance().updateRegisterFile(cdb);
    	}
    }
    
    /** clearReservationStations - look through all of the reservation stations of the functional units; 
     *    if any instructions have already written their results, evict them to make room.
     */
    public void clearReservationStations()
    {
    	branchUnit.clear();
    	memUnit.clear();
    	fdiv.clear();
    	fmult.clear();
    	fadder.clear();
    	intUnit.clear();
    }
    
    /** write - A functional unit's results are written to the CDB according to priority specifications.
     *    Only one result from one functional unit can be written to the CDB per cycle.
     *  @return - Common Data Bus
     */
    public CDB write()
    {
    	// Order: Memory Unit, Floating Point Divide, Floating Point Multiply, Floating Point Adder, Integer Unit
    	branchUnit.write();
    	CDB cdb = memUnit.write();
    	if (cdb != null)
    		return cdb;
    	cdb = this.fdiv.write();
    	if (cdb != null) 
    		return cdb;
    	cdb = this.fmult.write();
    	if (cdb != null) 
    		return cdb;
    	cdb = fadder.write();
    	if (cdb != null) 
    		return cdb;
    	cdb = intUnit.write();
    	if (cdb != null)
    		return cdb;
    	return null;
    }
    
    /** execute - calls the execute stage of the various functional units. If a branch instruction is determined in this
     *    stage, branchUnit's execute method returns true and we pass it back to the main simulator loop.
     *  @return true if branch was executed, false otherwise.
     */
    public boolean execute()
    {
    	memUnit.execute();
    	fdiv.execute();
    	fmult.execute();
    	fadder.execute();
    	intUnit.execute();
    	if (branchUnit.execute())
    		return true;
    	return false;
    }
    
    /** dump - dump various functional units and registers according to the passed in parameter.
     *  @param dump - value determining what to dump.
     */
    void dump(int dump)
    {
    	int mask = 1;
    	if ((dump & mask) == 1)
    		Memory.getInstance().dump();
    	dump = dump >> 1;
    	if ((dump & mask) == 1)
    		GPR.getInstance().dump();
    	dump = dump >> 1;
    	if ((dump & mask) == 1)
    		FPR.getInstance().dump();
    	dump = dump >> 1;
    	if ((dump & mask) == 1)
    		fadder.dump();
    	dump = dump >> 1;
    	if ((dump & mask) == 1)
    		fmult.dump();
    	dump = dump >> 1;
    	if ((dump & mask) == 1)
    		fdiv.dump();
    	dump = dump >> 1;
    	if ((dump & mask) == 1)
    		intUnit.dump();
    	dump = dump >> 1;
    	if ((dump & mask) == 1)
    		memUnit.dump();
    	dump = dump >> 1;
    	if ((dump & mask) == 1)
    		StatusTable.getInstance().dump();
    }
    
    /** issue - The issue step will decode the fetched instruction and issue the instruction to the appropriate group 
     *    of reservation stations. If each reservation station in the group is busy, the issue fails and is reattempted 
     *    in the next clock cycles. Halt, dump, and nop instructions are not issued to a reservation station. 
     * @param operation
     * @return - boolean indicating whether the processor should be stalled. 
     */
    public boolean issue(int operation)
    {
    	// A-35 - Halt not getting called
    	int opcode = Tools.grabBits(operation, 0, 5);
    	int rs = Tools.grabBits(operation, 6, 10);
    	int rt = Tools.grabBits(operation, 11, 15);
    	int imm = Tools.grabBits(operation, 16, 31);
    	int rd = Tools.grabBits(operation, 16, 20);
    	int func = Tools.grabBits(operation, 26, 31);
    	int offset = Tools.grabBits(operation, 6, 31);
    	switch (opcode)
    	{
        case 1:
            StatusTable.getInstance().addInstruction("halt", "HALT");
            halt = true;
            return true;
        case 4:
            return branchUnit.insertInstruction("beq", rs, rt, imm, offset);
        case 5:
            return branchUnit.insertInstruction("bne", rs, rt, imm, offset);
        case 2:
            return branchUnit.insertInstruction("j", rs, rt, imm, offset);
        case 53:
        	return memUnit.insertInstruction("l.d", rs, rt, imm);
        case 55:
            return memUnit.insertInstruction("ld", rs, rt, imm);
        case 61:
            return memUnit.insertInstruction("s.d", rs, rt, imm);
        case 63:
            return memUnit.insertInstruction("sd", rs, rt, imm);
    	case 24:
    		return intUnit.insertImmInstr("daddi", rs, rt, imm);
    	case 25:
            return intUnit.insertImmInstr("daddiu", rs, rt, imm);
        case 44:
            StatusTable.getInstance().addInstruction("dump " + offset, "DUMP");
            dump(offset);
            return false;
    	default:
    		switch(func)
    		{
    	    case 49:
    	        return fmult.insertInstruction("mul.d", rd, rs, rt);
    	    case 50:
    	        return fdiv.insertInstruction("div.d", rd, rs, rt);
    		case 44:
    			return intUnit.insertInstruction("dadd", rd, rs, rt);
    		case 46:
    			return intUnit.insertInstruction("dsub", rd, rs, rt);
    	    case 47:
    	        return fadder.insertInstruction("add.d", rd, rs, rt);
    	    case 48:
    	        return fadder.insertInstruction("sub.d", rd, rs, rt);
    		}
    	}
    	return false;
    }
    
    /** getPC - getter for PC, returns present PC value
     * @return - Program counter
     */
    public static int getPC()
    {
    	return PC;
    }
    
    /** setPC - static setter for PC, allows BranchUnit to update PC upon taken branch.
     *  @param address - address to set the PC to.
     */
    public static void setPC(int address)
    {
    	PC = address;
    }

    //This method is for the GUI, do not modify this
    public void addSnapShot(int instr, int PCValue, 
                            long cdbValue, String cdbSrc)
    {
         if (snapshots == null) snapshots = new ArrayList<CycleSnapShot>();
          
          snapshots.add(new CycleSnapShot(Clock.getInstance().get(), instr,
                        PCValue, buildFunctionalUnitImageList(), cdbValue,
                        cdbSrc));
     }

     //You'll need to modify this method to use the GUI
     private ArrayList<FUnitImage> buildFunctionalUnitImageList()
     {
          ArrayList<FUnitImage> list = new ArrayList<FUnitImage>();

          //you'll need to create a new FUnitImage object for each
          //of your Functional Units and pass that object to list.add
          //Each Functional Unit will have an array of Stations,
          //a count of the Stations, an executionCount, the currentInstruction,
          //a flag indicating whether it is busy or not, and the
          //number of remaining execution Cycles
          //
          //Here is a sample call, although it won't work until there
          //is an intUnit object.
/*
          list.add(new FUnitImage("integer", intUnit.RS, intUnit.RScount,
                   intUnit.executionCount, intUnit.currentInstruction,
                   intUnit.FUbusy, intUnit.executionCycles));

*/
          return list;
     }
}


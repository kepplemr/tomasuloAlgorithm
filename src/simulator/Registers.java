package simulator;

/** 
 * @author Michael Kepple
 * @version November 30th, 2012
 */
public class Registers
{
    protected long[] Regs; 
    public String[] Qi;
    static final int REGS = 32;
    
    public Registers()
    {
        int i;
        Regs = new long[REGS];
        Qi = new String[REGS];
        for (i = 0; i < REGS; i++)
        {
            Regs[i] = 0;
            Qi[i] = null;
        }
    }
    
    /** isFree - determines if the register is available by checking the Qi field in the register
     *    file. If another reservation station is computing a result bound for this register, the name
     *    of the station will be stored in the Qi String array.
     * @param regNum - index of the register to check.
     * @return true if available, false otherwise.
     */
    public boolean isFree(int regNum)
    {
    	return ((Qi[regNum] == null) ? true : false);
    }
    
    /** getRegister - returns the contents of the specified register
     * @param regNum - register number to grab
     * @return - contents of the specified register.
     */
    public long getRegister(int regNum)
    {
    	return Regs[regNum];
    }
    
    /** setRegister - sets designated register to designated value. Clears Qi field for that register
     *    because correct value is in the register itself.
     * @param reg - register to set
     * @param value - value to set it to.
     */
    public void setRegister(int reg, long value)
    {
    	// MIPS r0 always holds the value zero.
    	if (this instanceof GPR && reg == 0)
    		return;
    	Regs[reg] = value;
    }
    
    /** getResStation - returns name of reservation station due to write to this register.
     *  @param regNum - register number
     *  @return - name of reservation station.
     */
    public String getResStation(int regNum)
    {
    	return Qi[regNum];
    }
    
    /** setResStation - denote that the specified Reservation Station is computing a result
     *    that will be stored in the indicated register.
     *  @param register - the register whose Qi field we want to set.
     *  @param resStation - the reservation station that is computing the value to be put here.
     */
    public void setResStation(int register, String resStation)
    {
    	Qi[register] = resStation;
    }
    
    /** updateRegisters - updates appropriate registers if data from CDB is bound for register
     * @param paramCDB - Common Data Bus input
     */
    public void updateRegisterFile(CDB cdb)
    {
    	// Only called once?
    	for (int i = 0; i < REGS; i++)
    	{
    		if ((Qi[i] != null) && (Qi[i].equals(cdb.station)))
    		{
    			Qi[i] = null;
    			setRegister(i, cdb.result);
    		}
    	}
    }

    //output contents of Register File
    public void dumpRow(int start, int count)
    {
        int i, k;
        i = start;
        for (k = 0; k < count; k++, i++)
        {
            if (Qi[i] != null)
                System.out.print(Tools.pad(Qi[i], 16, " ", Direction.RIGHT) + " ");
            else
                System.out.print(Tools.pad(Long.toHexString(Regs[i]), 16, "0",
                                 Direction.RIGHT) + " ");
        }
        System.out.println();
    }

    //These two functions are used by the GUI
    public long[] cloneRegs()
    {
        return Regs.clone();
    }

    public String[] cloneQi()
    {
        return Qi.clone();
    }
}

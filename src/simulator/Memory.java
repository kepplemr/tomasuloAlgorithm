package simulator;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/** 
 * @author Michael Kepple
 * @version November 30th, 2012
 */
public class Memory
{
    private static Memory mem;
    private int [] memory;
    private int size;

    private Memory(int size)
    {
        this.size = size;
        memory = new int[this.size];
    }

    public static Memory getInstance()
    {
        if (mem == null) mem = new Memory(4000);
        return mem;
    }
    
    /** loadMemory - initializes memory based off of input hex file
     *  @param fileName - name of the file, already validated in Tomasulo class.
     *  @throws IOException
     */
    public void loadMemory(String fileName) throws IOException
    {
    	BufferedReader reader = new BufferedReader(new FileReader(fileName));
    	int memLocation = 0;
		String instruction = reader.readLine();
    	while (instruction != null)
    	{
    		instruction = instruction.substring(0, 8);
    		// parseInt can't handle overflow in Java.
    		long encoding = Long.parseLong(instruction, 16);
    		int fromLong = (int) encoding;
    		memory[(memLocation/4)] = fromLong;
    	    instruction = reader.readLine();
    	    memLocation += 4;
    	}
    	reader.close();
    }
    
    /** getWord - grabs 32 bits from specified memory location.
     * @param location - memory address to fetch
     * @return - contents of specified memory location.
     */
    public int getWord(int location)
    {
    	int word = -1;
    	if ((location % 4 == 0) && (location >= 0) && (location < size*4))
    		word = memory[(location/4)];
    	else
    	{
    		System.out.println("Error: unallowed memory access attempt:");
    		System.out.println("Location: " + location);
    		System.exit(1);
    	}
    	return word;
    }

    
    /** getLong - grabs 64 bits from specified memory location.
     * @param location - memory address to fetch
     * @return - contents of specified memory location.
     */
    
    public long getLong(long location)
    {
    	long result = -1;
    	if ((location % 8 == 0) && (location >= 0) && (location < size*4))
    	{
    		int first = this.memory[(((int)location + 4) / 4)];
    		int second = this.memory[((int)location / 4)];    	    
    		result = ((long)first << 32) | ((long)second & 0xFFFFFFFFL);
    	}
    	else
    	{
    		System.out.println("Error: unallowed memory access attempt.");
    		System.out.println("Location: " + location);
    		System.exit(1);
    	}
    	return result;
    }
    
    /** putLong - stores a 64 bit long value at a specified memory location.
     *  @param location - memory address to store value at.
     *  @param value - value to store at specified location in memory.
     */
    public void putLong(long location, long value)
    {
    	if ((location % 8 == 0) && (location >= 0) && (location <= size*4))
    	{
    		int first = (int)(value & 0xFFFFFFFF);
    		int second = (int)(value >> 32 & 0xFFFFFFFF);
    		memory[((int)location / 4)] = first;
    		memory[(((int)location + 4) / 4)] = second;
    	}
    	else
    	{
    		System.out.println("Error: unallowed memory access attempt::");
    		System.out.println("Location: " + location);
    		System.exit(1);
    	}
    }
 
    //helper function for dumping memory
    private String buildLine(int i)
    {
        String line;
        int j;
        line = new String();

        for (j = i; j < i + 8; j++)
        {
            line = line + Tools.pad(Integer.toHexString(memory[j]), 8, 
                                    "0", Direction.RIGHT) + " ";
        }
        return line;
    }

    //output contents of memory
    public void dump()
    {
        int address = 0;
        String lastline = new String("junk");
        String nextline;
        boolean star = false, needNewline = false;
        for (int i = 0; i < memory.length; i+=8)
        {
            nextline = buildLine(i);
            if (! lastline.equals(nextline))
            {
                star = false;
                if (needNewline) System.out.println(); 
                System.out.print(Tools.pad(Integer.toHexString(address), 4, "0",
                                           Direction.RIGHT) + ":\t");
                System.out.print(nextline);  
                needNewline = true;
            } else if (lastline.equals(nextline) && (star == false))
            {
               System.out.println(" *");
               needNewline = false;
               star = true;
            } 
            address = address + 32;
            lastline = nextline;
        }
        System.out.println();
    }

    //needed by the GUI
    public int[] cloneMemory()
    {
        return memory.clone();
    }

}



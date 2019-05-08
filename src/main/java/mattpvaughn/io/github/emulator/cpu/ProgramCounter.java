package mattpvaughn.io.github.emulator.cpu;

// Program counter for the ostrich GameBoy emulator
//
// The program counter keeps track of the address of the instruction which is
// currently being executed. It has settings to reset, increment, and goto
// lines.
// By Matt Vaughn: http://mattpvaughn.github.io/

public class ProgramCounter {

    // The current addr in the program counter
    private int addr;

    // The total size of the memory unit, also the last readable/writeable byte
    // in memory
    private static final int MAX_MEMORY_SIZE = 0xFFFF;

    public ProgramCounter() {
        this.addr = 0;
    }

    // Increments current byte to be read
    public void increment() {
        this.addr = this.addr++;
    }

    // Changes the current instruction to be executed at a certain line
    public void setAddr(int addr) {
        if (addr > MAX_MEMORY_SIZE) {
            throw new IllegalArgumentException("Address out of bounds");
        }
        this.addr = addr;
    }

    // Totally resets the PC back to 0, likely to run the boot rom again
    public void reset() {
        this.addr = 0;
    }

    public int getAddr() {
        return this.addr;
    }

    // Gets the current address then increments the current address number
    public int getAddrInc() {
        return this.addr++;
    }
}

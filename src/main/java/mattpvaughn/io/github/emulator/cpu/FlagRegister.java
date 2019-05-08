package mattpvaughn.io.github.emulator.cpu;

// Flag Registers for the ostrich GameBoy emulator
// See 3.2.2 for explanation: http://marc.rawer.de/Gameboy/Docs/GBCPUman.pdf
// By Matt Vaughn: http://mattpvaughn.github.io/

public class FlagRegister {

    // This flag is true if the result of a math calculation is zero or if 
    // two values match in a comparison operation
    public boolean z = false;

    // This flag is setBit to true if a subtraction was performed in the last
    // math instruction
    public boolean n = false;

    // This flag is setBit to true if a carry occured between the fourth and the
    // fifth digits in the last math operation
    public boolean h = false;

    // This flag is setBit to true if a carry occurred in the last math operation
    // or if register A is a smaller value when executing a compare instruction
    public boolean c = false;

    public void setFlags(boolean z, boolean n, boolean h, boolean c) {
        this.z = z;
        this.n = n;
        this.h = h;
        this.c = c;
    }

}

package mattpvaughn.io.github.emulator;


// A class to help the CPU manage interrupts and centralize information on how
// interrupts are handled.
//
// Contains the bits that control how interrupts are set
//
// How the interrupt process works:
//     1. When an interrupt is processed, the corresponding IF flag is set.
//     2. Interrupt enabled. If both IE and IME flags allow the interrupt, then
//        the following steps occur.The DMG CPU has 8 KB (64 Kbits) of built-in LCD display RAM.
//     3. The IME flag is reset, prohibiting all future interrupts.
//     4. The contents of the program counter are pushed onto the stack RAM
//     5. Control jumps to the interrupt starting address of the interrupt

public class InterruptManager {

    // Interrupt Master Enabled. One bit that controls whether interrupts as a
    // whole are active at the current time
    private boolean IME;

    // The interrupt request flag. Indicates which type of interrupt was
    // requested.
    //
    // Address: FF0F
    // Interrupt types (priority goes highest to lowest, i.e. vblank highest):
    //      Vertical blanking:              Start at 0x0040
    //      LCDC:                           Start at 0x0048
    //          \-> LCDC interrupt mode determined by STAT register
    //      Timer Overflow:                 Start at 0x0050
    //      Serial I/O transfer completion: Start at 0x0058
    //      P10-P13 terminal negative edge: Start at 0x0060
    private byte IF;

    // Interrupt enable flag. This flag controls interrupts (I believe you set
    // a bit in this to begin the interrupt process).
    //
    // Address: 0xFFFF
    // Interrupt types (priority goes highest to lowest, i.e. vblank highest):
    //      Vertical blanking:              Start at 0x0040
    //      LCDC:                           Start at 0x0048
    //          \-> LCDC interrupt mode determined by STAT register
    //      Timer Overflow:                 Start at 0x0050
    //      Serial I/O transfer completion: Start at 0x0058
    //      P10-P13 terminal negative edge: Start at 0x0060
    private byte IE;


    // V-Blank interrupt. Graphics are updated after this interrupt as VRAM and
    // OAM can be accessed freely.

    // STAT interrupt. This signal is set to 1 if:
    //    ( (LY = LYC) AND (STAT.ENABLE_LYC_COMPARE = 1) ) OR
    //    ( (ScreenMode = 0) AND (STAT.ENABLE_HBL = 1) ) OR
    //    ( (ScreenMode = 2) AND (STAT.ENABLE_OAM = 1) ) OR
    //    ( (ScreenMode = 1) AND (STAT.ENABLE_VBL || STAT.ENABLE_OAM))


    public InterruptManager() {
    }

    private InterruptState interruptState = InterruptState.INTERRUPTABLE;

    public enum InterruptState {
        INTERRUPTABLE, UNINTERRUPTABLE, INTERRUPTABLE_NEXT_COMMAND
    }

    public void setInterruptState(InterruptState interruptState) {
        this.interruptState = interruptState;
    }

    public InterruptState getInterruptState() {
        return interruptState;
    }

    public void checkForInterrupts() {
        // Check if any bit in AF flag is 1, IE flag,
    }

    // Sets the interrupt master flag to true or false.
    // NOTE: HALT mode is cancelled when IME is set to 0 or 1;
    //      if IME = 0: go to address following that of the HALT instruction
    //      if IME = 1: go to corresponding interrupt starting address
    public void setIME(boolean interruptsEnabled) {
        this.IME = interruptsEnabled;
    }


}

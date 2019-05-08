package mattpvaughn.io.github.emulator.cpu;

// CPU for the ostrich GameBoy emulator
// By Matt Vaughn: http://mattpvaughn.github.io/

import mattpvaughn.io.github.emulator.InterruptManager;
import mattpvaughn.io.github.emulator.Memory;

import static mattpvaughn.io.github.emulator.InterruptManager.InterruptState.*;

public class CPU {

    // Memory which CPU has access for reading/writing
    private Memory memory;

    // Component to keep track of program location
    private ProgramCounter pc;

    // Registers available for the CPU to use
    private CPURegister cpuRegister;

    // The flags in the CPU (z, h, N, C)
    private FlagRegister flagRegister;

    // Check for interrupts
    private InterruptManager interruptManager;

    // The number of CPU cycles elapsed since the start of the emulator
    private long cycles = 0;

    // Tracks number number of CPU cycles elapsed since the start of the
    // emulator. Increment DIV by 1 every 256 cycles.
    private int DIV = 0xFF04;

    // The current run status of the CPU
    private CPUState cpuState = CPUState.NORMAL;

    public enum CPUState {
        NORMAL, HALTED, STOPPED
    }

    public CPUState getCpuState() {
        return cpuState;
    }

    // Returns the current state of interrupts
    // Cycles: 0
    // Bytes consumed: 0
    InterruptManager.InterruptState getInterruptState() {
        return interruptManager.getInterruptState();
    }

    private CPU(CPURegister cpuRegister, ProgramCounter pc, FlagRegister flagRegister, Memory memory, InterruptManager interruptManager) {
        this.cpuRegister = cpuRegister;
        this.pc = pc;
        this.flagRegister = flagRegister;
        this.memory = memory;
        this.interruptManager = new InterruptManager();
    }

    // Builder pattern: useful when you have a number of potentially optional
    // parameters in an objects constructor.
    public static class Builder {
        private CPURegister cpuRegister;
        private ProgramCounter programCounter;
        private FlagRegister flagRegister;
        private Memory memory;
        private InterruptManager interruptManager;

        public Builder cpuRegister(CPURegister cpuRegister) {
            this.cpuRegister = cpuRegister;
            return this;
        }

        public Builder programCounter(ProgramCounter programCounter) {
            this.programCounter = programCounter;
            return this;
        }

        public Builder flagRegister(FlagRegister flagRegister) {
            this.flagRegister = flagRegister;
            return this;
        }

        public Builder memory(Memory memory) {
            this.memory = memory;
            return this;
        }

        public CPU build() {
            if (this.cpuRegister == null) {
                this.cpuRegister = new CPURegister();
            }
            if (this.programCounter == null) {
                this.programCounter = new ProgramCounter();
            }
            if (this.flagRegister == null) {
                this.flagRegister = new FlagRegister();
            }
            if (this.memory == null) {
                this.memory = new Memory();
            }
            if (this.interruptManager == null) {
                this.interruptManager = new InterruptManager();
            }
            return new CPU(
                    cpuRegister,
                    programCounter,
                    flagRegister,
                    memory,
                    interruptManager);
        }

    }

    // Returns whether there are more instructions in ROM to be executed
    public boolean hasInstruction() {
        return pc.getAddr() <= Memory.MAX_ROM_SIZE;
    }

    // Executes the current instruction in ROM, then returns the number of the
    // current cycle
    public long executeInstruction() {
        // Ensure that memory is available
        if (memory == null) {
            throw new IllegalStateException("Memory must be attached before instructions can be executed");
        }

        // Check for interrupts
        interruptManager.checkForInterrupts();

        // A V-blank occurs every 70224 clock cycles

        // Execute instruction, increment program counter
        cycles += Instructions.parse(memory.readRom(pc.getAddrInc()), this);

        // Update interrupt status
        if (interruptManager.getInterruptState().equals(INTERRUPTABLE_NEXT_COMMAND)) {
            interruptManager.setInterruptState(INTERRUPTABLE);
        }

        return cycles;
    }

    // Pop two bytes from the stack, jump to that address, then enable
    // interrupts. Note: interrupts enabled as soon as this command executes.
    //
    // Cycles: 8
    // Bytes consumed: 0
    public int reti() {
        // Pop two bytes off the stack, then jump to that address
        ret();

        // Enable interrupts
        interruptManager.setInterruptState(INTERRUPTABLE);

        return 8;
    }

    // Return if the z flag == cBoolean
    //
    // Cycles: 8
    // Bytes consumed: 0
    public int retC(boolean cBoolean) {
        if (cBoolean == flagRegister.c) {
            ret();
        }
        return 8;
    }

    // Return if the z flag == zBoolean
    //
    // Cycles: 8
    // Bytes consumed: 0
    public int retZ(boolean zBoolean) {
        if (zBoolean == flagRegister.z) {
            ret();
        }
        return 8;
    }

    // Pop two bytes from the stack then jump to that address
    //
    // Cycles: 8
    // Bytes consumed: 0
    public int ret() {
        // Pop 2 bytes from the stack
        byte[] values = pop166BitValue();

        // Turn the bytes into an address
        int address = Util.unsignedShortToInt(
                Util.concatBytes(values[0], values[1]));

        // Jump to that address
        pc.setAddr(address);

        return 8;
    }

    // Push present address to the stack- jump to address ($0000 + n)
    //
    // Cycles: 32
    // Bytes consumed: 0
    public int rest(byte n) {
        // Push current address
        push16BitValue(Util.splitShortToBytes((short) pc.getAddr()));

        // Jump to address (0x0000 + n)
        pc.setAddr(n);

        return 32;
    }

    // CALL address (firstByte, secondByte) if C Flag == cBoolean
    //
    // Cycles: 12
    // Bytes consumed: 2
    public int callCFlag(boolean cBoolean) {
        if (cBoolean == flagRegister.c) {
            return call();
        }
        return 12;
    }

    // CALL address (firstByte, secondByte) if z Flag == zBoolean
    //
    // Cycles: 12
    // Bytes consumed: 2
    public int callZFlag(boolean zBoolean) {
        if (zBoolean == flagRegister.z) {
            return call();
        }
        return 12;
    }

    // CALL nn: Push address of next instruction onto stack and then jump to
    // address nn
    //
    // Cycles: 12
    // Bytes consumed: 2
    public int call() {
        // Grab in reverse order b/c DMG is little-endian
        byte addr2 = memory.readRom(pc.getAddrInc());
        byte addr1 = memory.readRom(pc.getAddrInc());

        int address = Util.unsignedShortToInt(
                Util.concatBytes(addr1, addr2));

        // TODO: this may only work for addresses up to (2^16)/2 as this short
        // is a signed value!
        // Push the address of the next instruction to the stack
        push16BitValue(Util.splitShortToBytes((short) pc.getAddr()));

        // Jump to address "address"
        pc.setAddr(address);

        return 12;
    }

    // Jump to (current address + n) if C flag matches cBoolean where n is a
    // signed 8-bit value (two's complement, so java impl. will work)
    //
    // Cycles: 8
    // Bytes consumed: 1
    public int jumpCFlagRelative(boolean cBoolean) {
        byte n = memory.readRom(pc.getAddrInc());

        // Only jump if the Z flag matches zBoolean
        if (flagRegister.c == cBoolean) {
            pc.setAddr(pc.getAddr() + n);
        }

        return 8;
    }

    // Jump to (current address + n) if Z flag matches zBoolean where n is a
    // signed 8-bit value (two's complement, so java impl. will work)
    // Cycles: 8
    // Bytes consumed: 1
    public int jumpZFlagRelative(boolean zBoolean) {
        byte b = memory.readRom(pc.getAddrInc());

        // Only jump if the Z flag matches zBoolean
        if (flagRegister.z == zBoolean) {
            pc.setAddr(pc.getAddr() + b);
        }

        return 8;
    }

    // Jump to (current address + n) where n is an 8-Bit number
    //
    // Cycles: 8
    // Bytes consumed: 1
    public int jumpRelative() {
        byte n = memory.readRom(pc.getAddrInc());
        pc.setAddr(pc.getAddr() + n);
        return 8;
    }

    // Jump to the address stored in 16-bit register RR
    //
    // Cycles: 4
    // Bytes consumed: 0
    public int jump(CPURegister.Register RR) {
        if (!RR.isDoubleRegister()) {
            throw new IllegalArgumentException("Impossible to jump to an 8-bit address");
        }

        pc.setAddr(Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(RR)));

        return 4;
    }

    // Jump to addr in memory represented by next two bytes if C flag matches
    // boolean cBoolean
    //
    // Cycles: 12
    // Bytes consumed: 2
    public int jumpCFlag(boolean cBoolean) {
        // Grab in reverse order b/c DMG is little-endian
        byte addr2 = memory.readRom(pc.getAddrInc());
        byte addr1 = memory.readRom(pc.getAddrInc());

        int address = Util.unsignedShortToInt(
                Util.concatBytes(addr1, addr2));
        if (cBoolean == flagRegister.c) {
            pc.setAddr(address);
        }

        return 12;
    }

    // Jump if z flag matches boolean zBoolean
    //
    // Cycles: 12
    // Bytes consumed: 2
    public int jumpZFlag(boolean zBoolean) {
        // Grab in reverse order b/c DMG is little-endian
        byte addr2 = memory.readRom(pc.getAddrInc());
        byte addr1 = memory.readRom(pc.getAddrInc());

        int address = Util.unsignedShortToInt(
                Util.concatBytes(addr1, addr2));
        if (zBoolean == flagRegister.z) {
            pc.setAddr(address);
        }

        return 12;
    }

    // Jump to 16-bit address
    //
    // Cycles: 12
    // Bytes consumed: 2
    public int jump() {
        // Grab in reverse order b/c DMG is little-endian
        byte addr2 = memory.readRom(pc.getAddrInc());
        byte addr1 = memory.readRom(pc.getAddrInc());

        // Set the program counter's address to the address resolved by
        // combining the two bytes
        int address = Util.unsignedShortToInt(
                Util.concatBytes(addr1, addr2));
        pc.setAddr(address);

        return 12;
    }

    // Set bit b in Register R to have value "bitValue"
    //
    // Cycles:
    //      default: 8
    //      (HL):    16
    // Flags: not affected
    // Bytes consumed: 0
    public int setBit(CPURegister.Register R, byte pos, boolean bitValue) {
        // Check that we only use bits 0-7
        if (pos < -1 || pos >= 8) {
            throw new IllegalArgumentException("Bit out of bounds: " + pos);
        }
        // Check that register is valid
        if (R.isDoubleRegister()) {
            throw new IllegalArgumentException("Cannot call setBit on a 16-bit register!");
        }

        int cycles = 8 + addValueIfHL(R, 8);
        byte value = read8BitRegisterValue(R);

        value = Util.setBitValue(value, pos, bitValue);

        write8BitRegisterValue(R, value);

        return cycles;
    }


    // Test bit b in register R. If bit b of register R is 0, set z = true
    //
    // Cycles:
    //      default: 4
    //      (HL):    12
    // Flags:
    //      z: Set true if bit b of register R equals 0
    //      n: Set false
    //      h: Set true
    //      c: Not affected
    // Bytes consumed: 0
    public int checkBit(CPURegister.Register R, byte pos) {
        // Check that we only use bits 0-7
        if (pos <= -1 || pos >= 8) {
            throw new IllegalArgumentException("Bit position out of bounds: " + pos);
        }
        // Check that register is valid
        if (R.isDoubleRegister()) {
            throw new IllegalArgumentException("Cannot call checkBit on a 16-bit register!");
        }

        int cycles = 4 + addValueIfHL(R, 8);
        byte value = read8BitRegisterValue(R);

        flagRegister.z = Util.checkBit(value, pos);
        flagRegister.n = false;
        flagRegister.h = true;

        return cycles;

    }

    // Shift register R's bits to the right. Set bit 7 to zero if shouldZero ==
    // true, otherwise, it retains its previous value.
    //
    // Cycles:
    //      default: 4
    //      (HL):    12
    // Flags:
    //      z: Set if result is zero
    //      n: Set false
    //      h: Set false
    //      c: contains old bit 0 data
    // Bytes consumed: 0
    public int shiftRight(CPURegister.Register R, boolean shouldZero) {
        byte value = read8BitRegisterValue(R);
        byte shifted = Util.setBitValue((byte) (value >> 1),
                (byte) 7,
                !shouldZero && Util.checkBit(value, (byte) 7));
        write8BitRegisterValue(R, shifted);

        flagRegister.setFlags(shifted == 0,
                false,
                false,
                Util.checkBit(value, (byte) 0));

        return 4 + addValueIfHL(R, 8);
    }

    // Shift register R's bits to the left. Set bit 0 to zero
    //
    // Cycles:
    //      default: 4
    //      (HL):    12
    // Flags:
    //      z: Set if result is zero
    //      n: Set false
    //      h: Set false
    //      c: contains old bit 7 data
    // Bytes consumed: 0
    public int shiftLeft(CPURegister.Register R) {
        byte value = read8BitRegisterValue(R);
        byte shifted = Util.setBitValue((byte) (value << 1),
                (byte) 0,
                false);
        write8BitRegisterValue(R, shifted);

        flagRegister.setFlags(shifted == 0,
                false,
                false,
                Util.checkBit(value, (byte) 7));

        return 4 + addValueIfHL(R, 8);
    }

    // Rotate register R's bits to the right with bit 0 moved to bit 7
    //
    // Cycles:
    //      default: 4
    //      (HL):    12
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set false
    //      c: contains old bit 0 data
    // Bytes consumed: 0
    public int rotateRightThroughCarry(CPURegister.Register R) {
        byte value = read8BitRegisterValue(R);
        cpuRegister.setRegister(R, rotateValue(value, false, false));
        return 4 + addValueIfHL(R, 8);
    }

    // Rotate register R's bits to the right with the old carry value put into
    // bit 7
    //
    // Cycles:
    //      default: 4
    //      (HL):    12
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set false
    //      c: contains old bit 0 data
    // Bytes consumed: 0
    public int rotateRight(CPURegister.Register R) {
        byte value = read8BitRegisterValue(R);
        cpuRegister.setRegister(R, rotateValue(value, false, true));
        return 4 + addValueIfHL(R, 8);
    }

    // Rotate register R's bits to the left with bit 7 being moved to bit zero
    //
    // Cycles:
    //      default: 4
    //      (HL):    12
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set false
    //      c: contains old bit 7 data
    // Bytes consumed: 0
    public int rotateLeftThroughCarry(CPURegister.Register R) {
        byte value = read8BitRegisterValue(R);
        cpuRegister.setRegister(R, rotateValue(value, true, false));
        return 4 + addValueIfHL(R, 8);
    }

    // Rotate register R's bits to the left, with the old carry value put into
    // bit zero.
    //
    // Cycles:
    //      default: 4
    //      (HL):    12
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set false
    //      c: contains old bit 7 data
    // Bytes consumed: 0
    public int rotateLeft(CPURegister.Register R) {
        byte value = read8BitRegisterValue(R);
        cpuRegister.setRegister(R, rotateValue(value, true, true));
        return 4 + addValueIfHL(R, 8);
    }

    // Rotate the bits in a byte, returning the rotated byte value.
    // Parameters:
    //      toLeft: if true, rotate to the left, if false, rotate to the right
    //      useCarry: if true, empty bit is set to the old carry value, if
    //                false, set it to the bit that was moved off the edge
    //
    // Cycles:
    //      default: 4
    //      (HL):    12
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set false
    //      c: contains old bit 7 data if rotating left, contains old bit 0
    //         data if rotating right
    // Bytes consumed: 0
    private byte rotateValue(byte b, boolean toLeft, boolean useCarry) {
        byte rotated;
        // Rotate to the left or the right
        if (toLeft) {
            boolean replacementBit = useCarry ?
                    flagRegister.c : Util.checkBit(b, (byte) 0);
            rotated = (byte) (b << 1);
            rotated = Util.setBitValue(rotated, (byte) 0, replacementBit);
        } else {
            boolean replacementBit = useCarry ?
                    flagRegister.c : Util.checkBit(b, (byte) 7);
            rotated = (byte) (b >> 1);
            rotated = Util.setBitValue(rotated, (byte) 7, replacementBit);
        }

        // Set flags
        flagRegister.setFlags(Util.isZero(rotated),
                false,
                false,
                Util.checkBit(b, (byte) (toLeft ? 7 : 0)));

        return rotated;
    }

    // Enable interrupts after the command after this one is executed
    //
    // Cycles: 4
    // Flags: none affected
    // Bytes consumed: 0
    public int enableInterrupts() {
        interruptManager.setInterruptState(INTERRUPTABLE_NEXT_COMMAND);
        return 4;
    }

    // Disable interrupts immediately
    //
    // Cycles: 4
    // Flags: none affected
    // Bytes consumed: 0
    public int disableInterrupts() {
        interruptManager.setInterruptState(UNINTERRUPTABLE);
        return 4;
    }

    // Halt CPU and LCD display until button pressed
    //
    // Cycles: 4
    // Flags: none affected
    // Bytes consumed: 1
    public int stop() {
        byte nextByte = memory.readRom(pc.getAddrInc());
        // TODO- decide if this is necessary- AFAIK there is no reason to
        // include this b/c we should stop before, but it is a part of the spec
        if (nextByte != 0x00) {
            throw new IllegalArgumentException("Started STOP command but second byte was not 00");
        }

        cpuState = CPUState.STOPPED;

        return 4;
    }

    // Powers down CPU until an interrupt occurs.
    //
    // Cycles: 4
    // Flags: none affected
    // Bytes consumed: 0
    public int halt() {
        cpuState = CPUState.HALTED;
        return 4;
    }

    // Does nothing except take 4 cycles
    //
    // Cycles: 4
    // Flags: none affected
    // Bytes consumed: 0
    public int noOperation() {
        return 4;
    }

    // Sets the "carry" flag to be true
    //
    // Cycles: 4
    // Flags:
    //      z: not affected
    //      n: set false
    //      h: set false
    //      c: set true
    // Bytes consumed: 0
    public int setCarryFlag() {
        flagRegister.c = true;
        flagRegister.n = false;
        flagRegister.h = false;
        return 4;
    }

    // Flips the "carry" flag
    //
    // Cycles: 4
    // Flags:
    //      z: not affected
    //      n: reset
    //      h: reset
    //      c: complemented
    // Bytes consumed: 0
    public int complementCarryFlag() {
        flagRegister.c = !flagRegister.c;
        flagRegister.n = false;
        flagRegister.h = false;
        return 4;
    }

    // Set the A register to be it's own complement. (A = flipBits(A))
    //
    // Cycles: 4
    // Flags:
    //      z: not affected
    //      n: set true
    //      h: set true
    //      c: not affected
    // Bytes consumed: 0
    public int complement() {
        cpuRegister.A = (byte) ~cpuRegister.A;
        flagRegister.n = true;
        flagRegister.h = true;
        return 4;
    }

    // Adjust register A so that a correct representation of Binary Coded
    // Decimal (BCD) is obtained. Intended to occur immediately after an
    // addition or subtraction operation on a BCD so this number can correct any
    // mistakes made by running a binary addition/subtraction algorithm on a
    // decimal number.
    //
    // Cycles: 4
    // Flags:
    //      z: set true if register A is zero
    //      n: not affected
    //      h: set false
    //      c: set true or reset depending on operation
    // Bytes consumed: 0
    public int daa() {
        // If C is set OR a > 0x99, add or subtract 0x60 depending on N, and
        // set C
        int acc = 0;
        // After addition, adjust for out of (decimal) bounds or for half carries
        if (!flagRegister.n) {
            // Adjust for out of bounds
            if (flagRegister.c || acc > 0x99) {
                acc += 0x60;
                flagRegister.c = true;
            }
            // Adjust for half carries
            if (flagRegister.h || (acc & 0x0F) > 0x09) {
                acc += 0x06;
            }
        } else {
            // After subtraction, adjust in case of borrow or half borrow
            if (flagRegister.c) {
                acc -= 0x60;
            }
            if (flagRegister.h) {
                acc -= 0x06;
            }
        }

        // Set carry flag if changes to A result in a carry
        boolean carry = (acc + cpuRegister.A) > 0xFFFF;
        flagRegister.c = carry;

        // Change A register value
        cpuRegister.A = (byte) (acc + cpuRegister.A);

        // Adjust is a is zeroed by daa
        flagRegister.z = (acc == 0);

        // H set false always
        flagRegister.h = false;

        return 4;
    }


    // Swap the lower and upper nibbles of value in 8-bit register R
    //
    // Cycles:
    //      default:    8
    //      (HL):       16
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set false
    //      c: set false
    // Bytes consumed: 0
    public int swap(CPURegister.Register R) {
        if (R.isDoubleRegister()) {
            throw new IllegalArgumentException("Cannot perform SWAP on a 16-bit register");
        }

        int cycles = 8 + addValueIfHL(R, 8);
        byte value = read8BitRegisterValue(R);

        // Swap the halves of the byte
        byte swapped = (byte) ((value >> 4) ^ (value << 4));
        flagRegister.setFlags(Util.isZero(swapped),
                false,
                false,
                false);

        write8BitRegisterValue(R, swapped);

        return cycles;

    }

    // Add 16-bit register RR to register HL
    //
    // Cycles: 8
    // Flags:
    //      z: set if sum is zero
    //      n: set false
    //      h: set true if carry from bit 11
    //      c: set true if carry from bit 15
    // Bytes consumed: 0
    public int add(CPURegister.Register HL, CPURegister.Register RR,
                   boolean addCarry) {

        int cycles = 8;
        int value = Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(RR));
        int valueHL = Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(HL));
        int valueCarry = Util.booleanToInt(addCarry);

        // A = R + A + addCarry. Cast to a byte to discard any unneeded bits
        short sum = (short) (value + valueHL + valueCarry);

        // A half carry means the sum of the 8th bits carries over, so we have
        // a carry if the sum of the 8 least significant digits exceeds 0xFF
        short maskBit12 = 0x0FFF;
        boolean halfCarry = ((value & maskBit12)
                + (valueHL & maskBit12)
                + (valueCarry & maskBit12)) > maskBit12;

        // A carry means we carry from the 8th digit in the byte to a "virtual"
        // 16th byte. We have a carry when the sum of all of our numbers is
        // greater than (0xFFFF: a full 16-bits)
        boolean carry = (valueCarry + value + valueHL) > 0xFFFF;

        // z flag:
        flagRegister.setFlags(sum == 0,
                false,
                halfCarry,
                carry);

        cpuRegister.set16BitRegister(HL, sum);

        return cycles;
    }

    // Decrement the register R.
    //
    // Cycles:
    //      default:        4
    //      BC,DE,HL,SP:    8
    //      (HL):           12
    // Flags (8-bit register):
    //      z: set if result is zero
    //      n: set true
    //      h: set if borrow from bit 4
    //      c: not affected
    // Flags (16-bit register): Not affected
    // Bytes consumed: 0
    public int dec(CPURegister.Register R) {
        int cycles = 4 + addValueIfHL(R, 8);
        if (R.isDoubleRegister()) {
            // Don't change any flags for the 16-bit operation
            short value = cpuRegister.get16BitRegisterValue(R);
            cpuRegister.set16BitRegister(R, (short) (value - 1));
            cycles = 8;
        } else {
            byte value = read8BitRegisterValue(R);

            // We will have to borrow from bit 4 in a if the bottom 4 bits in the
            // subtracted number are greater than the bottom 4 bits in a
            boolean halfBorrow = (1 & 0x0F) > (value & 0x0F);

            // Set flags
            flagRegister.z = (value - 1) == 0;
            flagRegister.n = true;
            flagRegister.h = halfBorrow;

            write8BitRegisterValue(R, (byte) (value - 1));
        }

        return cycles;
    }

    // Increment the register R.
    //
    // Cycles:
    //      default:        4
    //      BC,DE,HL,SP:    8
    //      (HL):           12
    // Flags (8-bit register):
    //      z: set if result is zero
    //      n: set false
    //      h: set if carry from bit 4
    //      c: not affected
    // Flags (16-bit register): Not affected
    // Bytes consumed: 0
    public int inc(CPURegister.Register R) {
        int cycles = 4 + addValueIfHL(R, 8);
        if (R.isDoubleRegister()) {
            // Don't change any flags for the 16-bit operation
            short value = cpuRegister.get16BitRegisterValue(R);
            cpuRegister.set16BitRegister(R, (short) (value + 1));
            cycles = 8;
        } else {
            byte value = read8BitRegisterValue(R);

            // Set flags
            boolean halfCarry = ((value & 0x0F) + (1 & 0x0F)) > 0x0F;
            flagRegister.z = (value + 1) == 0;
            flagRegister.n = false;
            flagRegister.h = halfCarry;

            write8BitRegisterValue(R, (byte) (value + 1));
        }

        return cycles;
    }


    // XOR register A with byte n, save results to A
    //
    // Cycles: 8
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set false
    //      c: set false
    // Bytes consumed: 1
    public int xor() {
        byte b = memory.readRom(pc.getAddrInc());
        xorValue(b);
        return 8;
    }

    // XOR register A with register R, save results to A
    //
    // Cycles:
    //      Default: 4
    //      (HL):    8
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set false
    //      c: set false
    public int xor(CPURegister.Register R) {
        int cycles = 4 + addValueIfHL(R, 4);
        byte value = read8BitRegisterValue(R);
        xorValue(value);
        return cycles;
    }

    // Set register A = register A XOR b
    // Cycles: 0
    // Bytes consumed: 0
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set false
    //      c: set false
    private void xorValue(byte b) {
        byte xorred = (byte) (cpuRegister.A ^ b);
        cpuRegister.setRegister(CPURegister.Register.A, xorred);
        flagRegister.setFlags(Util.isZero(xorred), false, false, false);
    }

    // Read a byte from one of the 8-bit registers or from a value in memory
    // addressed by the value in the HL register
    private byte read8BitRegisterValue(CPURegister.Register R) {
        if (R.isDoubleRegister()) {
            throw new IllegalArgumentException("Cannot read 16-bit register");
        }

        if (R.equals(CPURegister.Register.HL_ADDRESS)) {
            return getByteFromHL();
        }
        return cpuRegister.get8BitRegisterValue(R);
    }

    // Write a byte to one of the 8-bit registers
    private void write8BitRegisterValue(CPURegister.Register R, byte b) {
        if (R.isDoubleRegister()) {
            throw new IllegalArgumentException("Cannot write 8-bit value to 16-bit register");
        }
        if (R.equals(CPURegister.Register.HL_ADDRESS)) {
            writeByteToHL(b);
        } else {
            cpuRegister.setRegister(R, b);
        }
    }

    // Return the int "value" if register R is the HL register
    private int addValueIfHL(CPURegister.Register R, int value) {
        return R.equals(CPURegister.Register.HL_ADDRESS) ? value : 0;
    }


    // OR register A with byte n, save results to A
    //
    // Cycles: 8
    // Flags:
    //      z: set if result is zero
    //      n: set false
    //      h: set false
    //      c: set false
    // Bytes consumed: 1
    public int or() {
        byte n = memory.readRom(pc.getAddrInc());
        orValue(n);
        return 8;
    }

    // OR register A with register R, save results to A
    //
    // Cycles:
    //      Default: 4
    //      HL:      8
    // Flags:
    //      z: set if result is zero
    //      n: set false
    //      h: set false
    //      c: set false
    // Bytes consumed: 0
    public int or(CPURegister.Register R) {
        int cycles = 4 + addValueIfHL(R, 4);
        byte value = read8BitRegisterValue(R);
        orValue(value);
        return cycles;
    }

    // register A = register A OR b
    // Flags: determined by public method
    // Cycles: 0
    // Bytes consumed: 0
    private void orValue(byte b) {
        byte orred = (byte) (b | cpuRegister.A);
        cpuRegister.setRegister(CPURegister.Register.A, orred);
        flagRegister.setFlags(Util.isZero(orred), false, false, false);
    }

    // AND register A with byte n, save results to register A
    //
    // Cycles: 8
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set true
    //      c: set false
    // Bytes consumed: 1
    public int and() {
        byte n = memory.readRom(pc.getAddrInc());
        andValue(n);
        return 8;
    }

    // AND register R with register A, save results to register A
    //
    // Cycles:
    //      default: 4
    //      HL:      8
    // Flags:
    //      z: set true if result is zero
    //      n: set false
    //      h: set true
    //      c: set false
    // Bytes consumed: 0
    public int and(CPURegister.Register R) {
        int cycles = 4 + addValueIfHL(R, 4);
        byte value = read8BitRegisterValue(R);
        andValue(value);
        return cycles;
    }

    // Set register A = register A & b
    // Cycles: 0
    // Flags: Set as determined by public methods
    // Bytes consumed: 0
    private void andValue(byte b) {
        byte anded = (byte) (b & cpuRegister.A);
        cpuRegister.A = anded;
        flagRegister.setFlags(Util.isZero(anded), false, true, false);
    }

    // Write a byte to the memory location located in the address stored in the
    // HL register
    private void writeByteToHL(byte b) {
        memory.writeByte(Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(CPURegister.Register.HL)), b);
    }

    // Read the byte from memory located at the address stored in the HL register
    private byte getByteFromHL() {
        return memory.readByte(Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(CPURegister.Register.HL)));
    }

    // Compare register A with byte n. Identical to SUB n but doesn't
    // write to A, just changes flags.
    //
    // Cycles: 8
    // Flags:
    //      z: set true if result is zero (A = n)
    //      n: set true
    //      h: set true if borrow from bit 4
    //      c: set true if borrow
    // Bytes consumed: 1
    public int cp() {
        int cycles = 4;
        byte b = memory.readRom(pc.getAddrInc());

        // This will subtract the numbers and set the flags correctly, but it
        // will change values in any 8-bit registers
        subValues(Util.unsignedByteToInt(cpuRegister.A),
                Util.unsignedByteToInt(b),
                0);

        return cycles;
    }

    // Compare register A with register R. Identical to SUB n but doesn't
    // write to A, just changes flags.
    //
    // Cycles:
    //      Default: 4
    //      (HL):    8
    // Flags:
    //      z: set true if result is zero (A = n)
    //      n: set true
    //      h: set true if no borrow from bit 4
    //      c: set true if A < n
    // Bytes consumed: 0
    public int cp(CPURegister.Register R) {
        if (R.isDoubleRegister()) {
            throw new IllegalArgumentException("add(Register, boolean) cannot handle 16-bit addition. Use add16Bit instead.");
        }
        int cycles = 4 + addValueIfHL(R, 4);
        byte value = read8BitRegisterValue(R);

        // Subtract the numbers, this also sets the flags accordingly
        subValues(
                Util.unsignedByteToInt(cpuRegister.A),
                Util.unsignedByteToInt(value),
                0);

        return cycles;
    }

    // Subtract register R from the A register. Subtract the carry flag as well
    // if borrow is true
    //
    // Cycles:
    //      default: 4
    //      HL:      8
    // Flags (GBCPUMAN defines this behavior incorrectly!):
    //      z: set true if result is zero
    //      n: set true
    //      h: set true if borrow from bit 4
    //      c: set true if borrow
    // Bytes consumed: 0
    public int sub(CPURegister.Register R, boolean borrow) {
        if (R.isDoubleRegister() && !R.equals(CPURegister.Register.HL)) {
            throw new IllegalArgumentException("add(Register, boolean) cannot handle 16-bit addition. Use add16Bit instead.");
        }
        int cycles = 4 + addValueIfHL(R, 4);
        byte value = read8BitRegisterValue(R);

        // Add the numbers, this will also set the flags correctly, as the spec
        // treats substitution as addition of negative numbers
        byte diff = subValues(
                Util.unsignedByteToInt(cpuRegister.A),
                Util.unsignedByteToInt(value),
                Util.booleanToInt(borrow));

        cpuRegister.setRegister(CPURegister.Register.A, diff);

        return cycles;
    }

    // Subtract b and c from a, returning the difference as a byte.
    // Flags:
    //      z: set true if result is zero
    //      n: set true
    //      h: set true if no borrow from bit 4
    //      c: set true if no borrow
    // Cycles: 0
    // Bytes consumed: 0
    private byte subValues(int a, int b, int c) {
        byte diff = (byte) (a - b - c);

        // We will have to borrow from bit 4 in a if the bottom 4 bits in the
        // subtracted number are greater than the bottom 4 bits in a
        boolean halfBorrow = ((b + c) & 0x0F) > (a & 0x0F);

        // It will overflow to negative if there is a borrow from "bit 8"
        boolean borrow = ((byte) (a - b - c)) < 0;

        // z flag:
        flagRegister.setFlags(diff == 0,
                true,
                halfBorrow,
                borrow);

        return diff;

    }

    // Subtract byte n from the A register.
    //
    // Cycles: 8
    // Flags:
    //      z: set if result is zero
    //      n: set true
    //      h: set true if no borrow from bit 4
    //      c: set true if no borrow
    // Bytes consumed: 1
    public int sub(boolean borrow) {
        byte b = memory.readRom(pc.getAddrInc());
        byte diff = subValues(cpuRegister.A, b, Util.booleanToInt(borrow));
        cpuRegister.setRegister(CPURegister.Register.A, diff);
        return 8;
    }

    // Add n to the A register. Add the half carry flag as well if addCarry is
    // true.
    //
    // Cycles: 8
    // Flags affected:
    //      z: set if result is zero
    //      n: reset
    //      h: set if carry from bit 3
    //      c: set if carry from bit 7
    // Bytes consumed: 1
    public int add(boolean addCarry) {
        byte b = memory.readRom(pc.getAddrInc());
        byte sum = addValues(
                Util.unsignedByteToInt(cpuRegister.A),
                Util.unsignedByteToInt(b),
                Util.booleanToInt(addCarry));
        cpuRegister.setRegister(CPURegister.Register.A, sum);
        return 8;
    }


    // Add the value at register R to the A register. Add the carry flag to A
    // as well if addCarry is true. Treat bytes as unsigned values
    //
    // Cycles:
    //      Default: 4
    //      HL:      8
    // Flags affected:
    //      z: set true if result is zero
    //      n: reset
    //      h: set true if carry from bit 3
    //      c: set true if carry from bit 7
    // Bytes consumed:
    //      default: 0
    //      SP:      1
    public int add(CPURegister.Register R, boolean addCarry) {
        if (R.isDoubleRegister()) {
            throw new IllegalArgumentException("add(Register, boolean) cannot handle 16-bit addition. Use add16Bit instead.");
        }
        int cycles = 4 + addValueIfHL(R, 4);

        // Get the value from register R
        byte value = read8BitRegisterValue(R);

        // Add the numbers, this also sets the flags accordingly
        byte sum = addValues(
                Util.unsignedByteToInt(value),
                Util.unsignedByteToInt(cpuRegister.A),
                Util.booleanToInt(addCarry));

        cpuRegister.setRegister(CPURegister.Register.A, sum);

        return cycles;
    }

    // Add 3 unsigned bytes: a, b, c.
    // Flags affected:
    //      z: set if result is zero
    //      n: reset
    //      h: set if carry from bit 3
    //      c: set if carry from bit 7
    // Cycles: 0
    // Bytes consumed: 0
    private byte addValues(int a, int b, int c) {
        // A = R + A + addCarry. Cast to a byte to discard any unneeded bits
        byte sum = (byte) (a + b + c);

        // A half carry means the sum of the 4th bits carries over, so we have
        // a carry if the sum of the 4 least significant digits exceeds 0x0F
        boolean halfCarry = ((a & 0x0F) + (b & 0x0F) + (c & 0x0F)) > 0x0F;

        // A carry means we carry from the 8th digit in the byte to a "virtual"
        // 9th byte. We have a carry when the sum of all of our numbers is
        // greater than (0b11111111/0xFF: a full byte)
        boolean carry = (a + b + c) > 0xFF;

        // z flag:
        flagRegister.setFlags(sum == 0,
                b < 0,
                halfCarry,
                carry);

        return sum;
    }

    // Pop two bytes off the stack into register pair RR. Increment SP twice
    // Cycles: 12
    // Bytes consumed: 0
    public int pop(CPURegister.Register RR) {
        // Pop two bytes off of the stack, increment SP
        byte[] values = pop166BitValue();

        // Put those bytes in 16-bit register RR
        cpuRegister.setRegister(RR, values[0], values[1]);

        return 12;
    }

    // Pop two bytes off of the stack, increment SP, return as byte[2]
    // Cycles: 0
    // Bytes consumed: 0
    private byte[] pop166BitValue() {
        // Get the address stored by the SP register
        int spAddress = Util.unsignedShortToInt(cpuRegister.SP);

        // Retrieve two bytes passed onto the stack.
        // Example for pop AF:
        //  1. read F
        //  2. inc SP
        //  3. read A
        //  4. inc SP
        byte[] values = new byte[]{
                memory.readByte(spAddress + 1), // Read A
                memory.readByte(spAddress)};    // Read F

        // Increment SP
        cpuRegister.set16BitRegister(CPURegister.Register.SP, (short) (spAddress + 2));

        return values;
    }

    // Decrement SP twice. Push the register pair XY onto the stack in the order
    // X, Y
    // Cycles: 16
    // Bytes consumed: 0
    public int push(CPURegister.Register RR) {
        // Get the values stored in RR
        byte[] values = Util.splitShortToBytes(cpuRegister.get16BitRegisterValue(RR));
        push16BitValue(values);
        return 16;
    }

    // Push a 16-bit value to the stack, decrement the stack pointer
    private void push16BitValue(byte[] values) {
        if (values.length != 2) {
            throw new IllegalArgumentException("Can only push two bytes to the stack");
        }

        // Get the address stored by the SP register
        int spAddress = Util.unsignedShortToInt(cpuRegister.SP);

        // Write the two bytes passed onto the stack.
        // Example for push AF:
        //  1. dec SP
        //  2. write A to SP
        //  3. dec SP
        //  4. write F to SP
        memory.writeByte(spAddress - 1, values[0]);
        memory.writeByte(spAddress - 2, values[1]);

        // Decrement the SP address by two
        cpuRegister.set16BitRegister(CPURegister.Register.SP, (short) (spAddress - 2));
    }


    // Write the value SP + n to register HL
    // Cycles: 12
    // Bytes consumed: 1
    public int writeSP8BitToHL() {
        byte n = memory.readRom(pc.getAddrInc());
        cpuRegister.set16BitRegister(CPURegister.Register.HL, (short) (cpuRegister.SP + n));
        return 12;
    }

    // Write the next byte in ROM into register R
    //
    // Cycles: 8
    // Bytes consumed: 1
    public int write8BitValueToRegister(CPURegister.Register R) {
        byte value = memory.readRom(pc.getAddrInc());
        cpuRegister.setRegister(R, value);
        return 8;
    }


    // Write a 16-bit value as specified by two java byte variables into register RR
    // Cycles: 12
    // Bytes consumed: 2
    public int write16BitValueToRegister(CPURegister.Register RR) {
        // Grab in reverse order b/c DMG is little-endian
        byte value2 = memory.readRom(pc.getAddrInc());
        byte value1 = memory.readRom(pc.getAddrInc());

        cpuRegister.setRegister(RR, value1, value2);

        return 12;
    }

    // TODO- the two below methods can probably be refactored into previous method by using constant 0xFF

    // Write from memory address ($FF00 + n) into register R
    // Cycles: 12
    // Bytes consumed: 1
    public int writeMemoryByteToRegister(CPURegister.Register R) {
        byte b = memory.readRom(pc.getAddrInc());

        // Write from address (0xFF00 + n) to register R
        cpuRegister.setRegister(R, memory.readByte(0xFF00 + b));

        return 12;
    }

    // Write from register R into memory at address ($FF00 + n)
    // Cycles: 12
    // Bytes consumed: 1
    public int writeRegisterToMemoryByte(CPURegister.Register R) {
        byte b = memory.readRom(pc.getAddrInc());
        int address = 0xFF00 + b;
        memory.writeByte(address, cpuRegister.get8BitRegisterValue(R));
        return 12;
    }

    // Write from register R to memory at addr held by register HL, adding the
    // value "value" to the register HL
    // Cycles: 8
    // Bytes consumed: 0
    public int writeFromRegisterToMemory(CPURegister.Register R, int value) {
        int cycles = 8;

        // Get address stored by HL
        int address = Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(CPURegister.Register.HL));

        // Write from register R to the address extracted from HL
        memory.writeByte(address, cpuRegister.get8BitRegisterValue(R));

        // Add the integer value to the HL register
        cpuRegister.set16BitRegister(CPURegister.Register.HL, (short) (
                cpuRegister.get16BitRegisterValue(CPURegister.Register.HL) + value));

        return cycles;
    }

    // Write from memory at address held by HL to Register R, adding the value
    // "value" to the register HL
    // Cycles: 8
    // Bytes consumed: 0
    public int writeMemoryToRegisterCrement(CPURegister.Register R, int value) {
        int cycles = 8;
        // Get address store by RR
        int address = Util.unsignedShortToInt(cpuRegister.get16BitRegisterValue(CPURegister.Register.HL));

        // Get value from that address, put into R register
        cpuRegister.setRegister(R, memory.readByte(address));

        // (In/De)crement the RR register
        cpuRegister.set16BitRegister(CPURegister.Register.HL, (short) (cpuRegister.get16BitRegisterValue(CPURegister.Register.HL) + value));

        return cycles;
    }

    // Write from register A to memory at address ($FF00 + register C)
    // Cycles: 8
    // Bytes consumed: 1
    public int writeAToC() {
        int cycles = 8;

        // We don't actually need the next bytes- it's easier to not read them
        // and just do 0xFF00 + C here. Increment to pretend like we read it
        pc.increment();

        // Form the address
        int address = 0xFF00 + cpuRegister.get8BitRegisterValue(CPURegister.Register.C);

        // Write the value at that address into register A
        memory.writeByte(address, cpuRegister.get8BitRegisterValue(CPURegister.Register.A));

        return cycles;
    }

    // Write value at address ($FF00 + register C) into A
    // Cycles: 8
    // Bytes consumed: 1
    public int writeCToA() {
        int cycles = 8;

        // We don't actually need the next bytes- it's easier to not read them
        // and just do 0xFF00 + C here. Increment to pretend like we read it
        pc.increment();

        // Form the address
        int address = 0xFF00 + cpuRegister.C;

        // Write the value at that address into register A
        cpuRegister.setRegister(CPURegister.Register.A, memory.readByte(address));

        return cycles;
    }

    // Write from memory stored in the address in the double register RR
    // to the register R
    //
    // Cycles: 8
    // Bytes consumed: 0
    public int writeMemoryToRegister(CPURegister.Register R, CPURegister.Register RR) {
        int cycles = 8;

        if (R.isDoubleRegister()) {
            throw new IllegalArgumentException("First argument to writeByteToRegister must be an 8-bit register");
        }

        if (!RR.isDoubleRegister()) {
            throw new IllegalArgumentException("Second argument to writeByteToRegister must be a 16-bit register");
        }

        int address = Util.unsignedShortToInt(cpuRegister.get16BitRegisterValue(RR));
        byte value = memory.readByte(address);
        cpuRegister.setRegister(R, value);

        return cycles;
    }

    // Write data from register "from" to register "to"
    // Cycles:
    //      default:    4 cycles
    //      (HL):       8 cycles
    // Bytes consumed: 0
    public int writeRegisterToRegister(CPURegister.Register from, CPURegister.Register to) {
        if (to.isDoubleRegister()) {
            throw new IllegalArgumentException("Cannot write 8-bit register to 16-bit register");
        } else if (from.isDoubleRegister()) {
            throw new IllegalArgumentException("Cannot write 16-bit register to 8-bit register");
        }

        int cycles = 4 + addValueIfHL(to, 4);
        write8BitRegisterValue(to, read8BitRegisterValue(from));

        return cycles;
    }

    // Write byte b into register R
    // Cycles:
    //      default: 8
    //      (HL):    12
    // Bytes consumed: 1
    public int writeByteToRegister(CPURegister.Register R) {
        if (R.isDoubleRegister()) {
            throw new IllegalArgumentException("Cannot write byte into two-byte register");
        }
        int cycles = 8 + addValueIfHL(R, 4);

        // Get n from ROM
        byte b = memory.readRom(pc.getAddrInc());
        write8BitRegisterValue(R, b);

        return cycles;
    }

    // Write from register to provided memory address
    // Cycles:
    //      A -> (nn):  16
    //      SP -> (nn): 20
    // Bytes consumed: 2
    public int writeRegisterToMemory(CPURegister.Register register) {
        int cycles = 16;
        if (register.equals(CPURegister.Register.SP)) {
            cycles += 4;
        }

        // Grab in reverse order b/c DMG is little-endian
        byte addr2 = memory.readRom(pc.getAddrInc());
        byte addr1 = memory.readRom(pc.getAddrInc());

        byte value;
        if (register.equals(CPURegister.Register.A)) {
            value = cpuRegister.get8BitRegisterValue(CPURegister.Register.A);
        } else if (register.equals(CPURegister.Register.SP)) {
            // Get the value from the address stored in SP
            short address = cpuRegister.get16BitRegisterValue(CPURegister.Register.SP);
            value = memory.readByte(Util.unsignedShortToInt(address));
        } else {
            throw new IllegalArgumentException("Cannot write from any register but SP or A directly to memory!");
        }

        memory.writeByte(Util.unsignedShortToInt(Util.concatBytes(addr1, addr2)), value);

        return cycles;
    }
}

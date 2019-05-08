package mattpvaughn.io.github.emulator.cpu;

// Instruction parser for the ostrich GameBoy emulator
//
// Statically parses 6-byte instructions from the ROM and calls the relevant
// methods in the CPU.
//
// By Matt Vaughn: http://mattpvaughn.github.io/

import mattpvaughn.io.github.emulator.Utils;

public class Instructions {

    // If true- an 0xCB command has just been issued, so we need to call
    // commands from the CB-prefixed command table. Also- no interrupts allowed
    private static boolean cbPending = false;

    // Parses a single command from a line of GameBoy assembly code, then executes it
    // Returns the number of cycles taken for the operation
    public static int parse(byte unsignedOpCode, CPU cpu) {
        // Parse the command using op codes
        Utils.log(String.format("%02X", unsignedOpCode));


        // The actual ROMs provide 8-bit unsigned ints while java uses signed ints, so
        // convert the byte to signed ints instead so we can write case statements as
        // 0x80-0xFF (as these would not fit in a byte)
        int code = unsignedOpCode;
        if (code < 0) {
            code = code + 256;
        }

        // Add 0xCB00 to the code if we are expecting a CB command
        if (cbPending) {
            cbPending = false;
            code = code + 0xCB00;
        }

        // Calculate the value used for the bit() function call
        byte bitValue = (byte) ((unsignedOpCode / 8) - 8);

        // Check if the command passed is a CB-prefixed command- if it is, we
        // should compare the following bytes against CB-prefixed commands
        switch (code) {
            // LD r, d: write from a memory addr d to register r
            case 0x3E:
                return cpu.write8BitValueToRegister(CPURegister.Register.A);
            case 0x06:
                return cpu.writeByteToRegister(CPURegister.Register.B);
            case 0x0E:
                return cpu.writeByteToRegister(CPURegister.Register.C);
            case 0x16:
                return cpu.writeByteToRegister(CPURegister.Register.D);
            case 0x1E:
                return cpu.writeByteToRegister(CPURegister.Register.E);
            case 0x26:
                return cpu.writeByteToRegister(CPURegister.Register.H);
            case 0x2E:
                return cpu.writeByteToRegister(CPURegister.Register.L);
            case 0x36:
                return cpu.write8BitValueToRegister(CPURegister.Register.HL_ADDRESS);

            // LD r1, r2: write data from register R2 into register R1
            case 0x7F:
                return cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.A);
            case 0x78:
                return cpu.writeRegisterToRegister(CPURegister.Register.B, CPURegister.Register.A);
            case 0x79:
                return cpu.writeRegisterToRegister(CPURegister.Register.C, CPURegister.Register.A);
            case 0x7A:
                return cpu.writeRegisterToRegister(CPURegister.Register.D, CPURegister.Register.A);
            case 0x7B:
                return cpu.writeRegisterToRegister(CPURegister.Register.E, CPURegister.Register.A);
            case 0x7C:
                return cpu.writeRegisterToRegister(CPURegister.Register.H, CPURegister.Register.A);
            case 0x7D:
                return cpu.writeRegisterToRegister(CPURegister.Register.L, CPURegister.Register.A);
            case 0x40:
                return cpu.writeRegisterToRegister(CPURegister.Register.B, CPURegister.Register.B);
            case 0x41:
                return cpu.writeRegisterToRegister(CPURegister.Register.C, CPURegister.Register.B);
            case 0x42:
                return cpu.writeRegisterToRegister(CPURegister.Register.D, CPURegister.Register.B);
            case 0x43:
                return cpu.writeRegisterToRegister(CPURegister.Register.E, CPURegister.Register.B);
            case 0x44:
                return cpu.writeRegisterToRegister(CPURegister.Register.H, CPURegister.Register.B);
            case 0x45:
                return cpu.writeRegisterToRegister(CPURegister.Register.L, CPURegister.Register.B);
            case 0x46:
                return cpu.writeRegisterToRegister(CPURegister.Register.HL_ADDRESS, CPURegister.Register.B);
            case 0x48:
                return cpu.writeRegisterToRegister(CPURegister.Register.B, CPURegister.Register.C);
            case 0x49:
                return cpu.writeRegisterToRegister(CPURegister.Register.C, CPURegister.Register.C);
            case 0x4A:
                return cpu.writeRegisterToRegister(CPURegister.Register.D, CPURegister.Register.C);
            case 0x4B:
                return cpu.writeRegisterToRegister(CPURegister.Register.E, CPURegister.Register.C);
            case 0x4C:
                return cpu.writeRegisterToRegister(CPURegister.Register.H, CPURegister.Register.C);
            case 0x4D:
                return cpu.writeRegisterToRegister(CPURegister.Register.L, CPURegister.Register.C);
            case 0x4E:
                return cpu.writeRegisterToRegister(CPURegister.Register.HL_ADDRESS, CPURegister.Register.C);
            case 0x50:
                return cpu.writeRegisterToRegister(CPURegister.Register.B, CPURegister.Register.D);
            case 0x51:
                return cpu.writeRegisterToRegister(CPURegister.Register.C, CPURegister.Register.D);
            case 0x52:
                return cpu.writeRegisterToRegister(CPURegister.Register.D, CPURegister.Register.D);
            case 0x53:
                return cpu.writeRegisterToRegister(CPURegister.Register.E, CPURegister.Register.D);
            case 0x54:
                return cpu.writeRegisterToRegister(CPURegister.Register.H, CPURegister.Register.D);
            case 0x55:
                return cpu.writeRegisterToRegister(CPURegister.Register.L, CPURegister.Register.D);
            case 0x56:
                return cpu.writeRegisterToRegister(CPURegister.Register.HL_ADDRESS, CPURegister.Register.D);
            case 0x58:
                return cpu.writeRegisterToRegister(CPURegister.Register.B, CPURegister.Register.E);
            case 0x59:
                return cpu.writeRegisterToRegister(CPURegister.Register.C, CPURegister.Register.E);
            case 0x5A:
                return cpu.writeRegisterToRegister(CPURegister.Register.D, CPURegister.Register.E);
            case 0x5B:
                return cpu.writeRegisterToRegister(CPURegister.Register.E, CPURegister.Register.E);
            case 0x5C:
                return cpu.writeRegisterToRegister(CPURegister.Register.H, CPURegister.Register.E);
            case 0x5D:
                return cpu.writeRegisterToRegister(CPURegister.Register.L, CPURegister.Register.E);
            case 0x5E:
                return cpu.writeRegisterToRegister(CPURegister.Register.HL_ADDRESS, CPURegister.Register.E);
            case 0x60:
                return cpu.writeRegisterToRegister(CPURegister.Register.B, CPURegister.Register.H);
            case 0x61:
                return cpu.writeRegisterToRegister(CPURegister.Register.C, CPURegister.Register.H);
            case 0x62:
                return cpu.writeRegisterToRegister(CPURegister.Register.D, CPURegister.Register.H);
            case 0x63:
                return cpu.writeRegisterToRegister(CPURegister.Register.E, CPURegister.Register.H);
            case 0x64:
                return cpu.writeRegisterToRegister(CPURegister.Register.H, CPURegister.Register.H);
            case 0x65:
                return cpu.writeRegisterToRegister(CPURegister.Register.L, CPURegister.Register.H);
            case 0x66:
                return cpu.writeRegisterToRegister(CPURegister.Register.HL_ADDRESS, CPURegister.Register.H);
            case 0x68:
                return cpu.writeRegisterToRegister(CPURegister.Register.B, CPURegister.Register.L);
            case 0x69:
                return cpu.writeRegisterToRegister(CPURegister.Register.C, CPURegister.Register.H);
            case 0x6A:
                return cpu.writeRegisterToRegister(CPURegister.Register.D, CPURegister.Register.H);
            case 0x6B:
                return cpu.writeRegisterToRegister(CPURegister.Register.E, CPURegister.Register.H);
            case 0x6C:
                return cpu.writeRegisterToRegister(CPURegister.Register.H, CPURegister.Register.H);
            case 0x6D:
                return cpu.writeRegisterToRegister(CPURegister.Register.L, CPURegister.Register.H);
            case 0x6E:
                return cpu.writeRegisterToRegister(CPURegister.Register.HL_ADDRESS, CPURegister.Register.H);
            case 0x70:
                return cpu.writeRegisterToRegister(CPURegister.Register.B, CPURegister.Register.HL_ADDRESS);
            case 0x71:
                return cpu.writeRegisterToRegister(CPURegister.Register.C, CPURegister.Register.HL_ADDRESS);
            case 0x72:
                return cpu.writeRegisterToRegister(CPURegister.Register.D, CPURegister.Register.HL_ADDRESS);
            case 0x73:
                return cpu.writeRegisterToRegister(CPURegister.Register.E, CPURegister.Register.HL_ADDRESS);
            case 0x74:
                return cpu.writeRegisterToRegister(CPURegister.Register.H, CPURegister.Register.HL_ADDRESS);
            case 0x75:
                return cpu.writeRegisterToRegister(CPURegister.Register.L, CPURegister.Register.HL_ADDRESS);
            case 0x47:
                return cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.B);
            case 0x4F:
                return cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.C);
            case 0x57:
                return cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.D);
            case 0x5F:
                return cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.E);
            case 0x67:
                return cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.H);
            case 0x6F:
                return cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.L);
            case 0x02:
                return cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.BC);
            case 0x12:
                return cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.DE);
            case 0x77:
                return cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.HL_ADDRESS);

            // LD (nn) -> A: write to register A from memory referred to by
            //               address in double register
            case 0x0A:
                return cpu.writeMemoryToRegister(CPURegister.Register.A, CPURegister.Register.BC);
            case 0x1A:
                return cpu.writeMemoryToRegister(CPURegister.Register.A, CPURegister.Register.DE);
            case 0x7E: // A -> (C)
                return cpu.writeMemoryToRegister(CPURegister.Register.A, CPURegister.Register.HL_ADDRESS);

            // LD (nn) -> A: write from memory location nn to register A
            case 0xFA:
                return cpu.writeByteToRegister(CPURegister.Register.A);

            // LD (C) -> A: Put value at address $FF00 + register C into register A
            case 0xF2:
                return cpu.writeCToA();

            // LD A -> (C): Write register A into address $FF00 + register C
            case 0xE2:
                return cpu.writeAToC();

            // LD A -> (nn): Write from a register to a memory location (nn)
            case 0xEA:
                return cpu.writeRegisterToMemory(CPURegister.Register.A);

            // LDD (HL) -> A: write from address at HL into A, decrement HL
            case 0x3A:
                return cpu.writeMemoryToRegisterCrement(CPURegister.Register.A,
                        -1);

            // LDD A -> (HL): write from register A into memory at address HL, decrement HL
            case 0x32:
                return cpu.writeFromRegisterToMemory(CPURegister.Register.A, -1);

            // LDI (HL) -> A: write from memory at address HL to register A, increment HL
            case 0x2A:
                return cpu.writeMemoryToRegisterCrement(CPURegister.Register.A, 1);

            // LDI A -> (HL): write from register A into memory at address HL, increment HL
            case 0x22:
                return cpu.writeFromRegisterToMemory(CPURegister.Register.A, 1);

            // LDH A -> (n): write from register A into memory at address ($FF00 + n)
            case 0xE0:
                return cpu.writeRegisterToMemoryByte(CPURegister.Register.A);

            // LDH (n) -> A: write from memory at address ($FF00 + n) to register A
            case 0xF0:
                return cpu.writeMemoryByteToRegister(CPURegister.Register.A);

            // LD nn -> n: write 16-bit immediate value into 16-bit register
            case 0x01:
                return cpu.write16BitValueToRegister(CPURegister.Register.BC);
            case 0x11:
                return cpu.write16BitValueToRegister(CPURegister.Register.DE);
            case 0x21:
                return cpu.write16BitValueToRegister(CPURegister.Register.HL);
            case 0x31:
                return cpu.write16BitValueToRegister(CPURegister.Register.SP);

            // LD HL -> SP
            case 0xF9:
                return cpu.writeRegisterToRegister(CPURegister.Register.HL, CPURegister.Register.SP);

            // LD (SP + n) -> (HL): Write from address (SP + n) into address stored in HL
            case 0xF8:
                return cpu.writeSP8BitToHL();

            // LD SP -> (nn): Write value at SP register to memory at address nn
            case 0x08:
                return cpu.writeRegisterToMemory(CPURegister.Register.SP);

            // PUSH RR: push register pair RR onto the stack, decrement SP twice
            case 0xF5:
                return cpu.push(CPURegister.Register.AF);
            case 0xC5:
                return cpu.push(CPURegister.Register.BC);
            case 0xD5:
                return cpu.push(CPURegister.Register.DE);
            case 0xE5:
                return cpu.push(CPURegister.Register.HL);

            // POP RR: pop two bytes off stack into register pair RR. Increment SP twice
            case 0xF1:
                return cpu.pop(CPURegister.Register.AF);
            case 0xC1:
                return cpu.pop(CPURegister.Register.BC);
            case 0xD1:
                return cpu.pop(CPURegister.Register.DE);
            case 0xE1:
                return cpu.pop(CPURegister.Register.HL);

            //////////////////////////////////////////
            //  ALU OPERATIONS  -  8-bit operations //
            //////////////////////////////////////////

            // Add A, n: add n to A
            case 0x87:
                return cpu.add(CPURegister.Register.A, false);
            case 0x80:
                return cpu.add(CPURegister.Register.B, false);
            case 0x81:
                return cpu.add(CPURegister.Register.C, false);
            case 0x82:
                return cpu.add(CPURegister.Register.D, false);
            case 0x83:
                return cpu.add(CPURegister.Register.E, false);
            case 0x84:
                return cpu.add(CPURegister.Register.H, false);
            case 0x85:
                return cpu.add(CPURegister.Register.L, false);
            case 0x86:
                return cpu.add(CPURegister.Register.HL_ADDRESS, false);
            case 0xC6:
                return cpu.add(false);


            // ADC A, n: Add n + carry flag to A
            case 0x8F:
                return cpu.add(CPURegister.Register.A, true);
            case 0x88:
                return cpu.add(CPURegister.Register.B, true);
            case 0x89:
                return cpu.add(CPURegister.Register.C, true);
            case 0x8A:
                return cpu.add(CPURegister.Register.D, true);
            case 0x8B:
                return cpu.add(CPURegister.Register.E, true);
            case 0x8C:
                return cpu.add(CPURegister.Register.H, true);
            case 0x8D:
                return cpu.add(CPURegister.Register.L, true);
            case 0x8E:
                return cpu.add(CPURegister.Register.A, true);
            case 0xCE:
                return cpu.add(true);

            // SUB R: subtract register R from A
            case 0x97:
                return cpu.sub(CPURegister.Register.A, false);
            case 0x90:
                return cpu.sub(CPURegister.Register.B, false);
            case 0x91:
                return cpu.sub(CPURegister.Register.C, false);
            case 0x92:
                return cpu.sub(CPURegister.Register.D, false);
            case 0x93:
                return cpu.sub(CPURegister.Register.E, false);
            case 0x94:
                return cpu.sub(CPURegister.Register.H, false);
            case 0x95:
                return cpu.sub(CPURegister.Register.L, false);
            case 0x96:
                return cpu.sub(CPURegister.Register.HL_ADDRESS, false);

            // SBC R: A = A - R - carry
            case 0x9F:
                return cpu.sub(CPURegister.Register.A, true);
            case 0x98:
                return cpu.sub(CPURegister.Register.B, true);
            case 0x99:
                return cpu.sub(CPURegister.Register.C, true);
            case 0x9A:
                return cpu.sub(CPURegister.Register.D, true);
            case 0x9B:
                return cpu.sub(CPURegister.Register.E, true);
            case 0x9C:
                return cpu.sub(CPURegister.Register.H, true);
            case 0x9D:
                return cpu.sub(CPURegister.Register.L, true);
            case 0x9E:
                return cpu.sub(CPURegister.Register.HL_ADDRESS, true);

            // AND R: A = A AND register R
            case 0xA7:
                return cpu.and(CPURegister.Register.A);
            case 0xA0:
                return cpu.and(CPURegister.Register.B);
            case 0xA1:
                return cpu.and(CPURegister.Register.C);
            case 0xA2:
                return cpu.and(CPURegister.Register.D);
            case 0xA3:
                return cpu.and(CPURegister.Register.E);
            case 0xA4:
                return cpu.and(CPURegister.Register.H);
            case 0xA5:
                return cpu.and(CPURegister.Register.L);
            case 0xA6:
                return cpu.and(CPURegister.Register.HL_ADDRESS);


            // OR R: A = A OR register R
            case 0xB7:
                return cpu.or(CPURegister.Register.A);
            case 0xB0:
                return cpu.or(CPURegister.Register.B);
            case 0xB1:
                return cpu.or(CPURegister.Register.C);
            case 0xB2:
                return cpu.or(CPURegister.Register.D);
            case 0xB3:
                return cpu.or(CPURegister.Register.E);
            case 0xB4:
                return cpu.or(CPURegister.Register.H);
            case 0xB5:
                return cpu.or(CPURegister.Register.L);
            case 0xB6:
                return cpu.or(CPURegister.Register.HL_ADDRESS);


            // XOR n: A = A XOR n
            case 0xAF:
                return cpu.xor(CPURegister.Register.A);
            case 0xA8:
                return cpu.xor(CPURegister.Register.B);
            case 0xA9:
                return cpu.xor(CPURegister.Register.C);
            case 0xAA:
                return cpu.xor(CPURegister.Register.D);
            case 0xAB:
                return cpu.xor(CPURegister.Register.E);
            case 0xAC:
                return cpu.xor(CPURegister.Register.H);
            case 0xAD:
                return cpu.xor(CPURegister.Register.L);
            case 0xAE:
                return cpu.xor(CPURegister.Register.HL_ADDRESS);

            // CP R: Compare A with register R. Identical to A - R but
            // results are thrown away
            case 0xBF:
                return cpu.cp(CPURegister.Register.A);
            case 0xB8:
                return cpu.cp(CPURegister.Register.B);
            case 0xB9:
                return cpu.cp(CPURegister.Register.C);
            case 0xBA:
                return cpu.cp(CPURegister.Register.D);
            case 0xBB:
                return cpu.cp(CPURegister.Register.E);
            case 0xBC:
                return cpu.cp(CPURegister.Register.H);
            case 0xBD:
                return cpu.cp(CPURegister.Register.L);
            case 0xBE:
                return cpu.cp(CPURegister.Register.HL_ADDRESS);

            // SBC n: A = A - n - carry
            case 0xDE:
                return cpu.sub(true);
            // SUB n: A = A - n
            case 0xD6:
                return cpu.sub(false);
            // AND n: A = A AND n
            case 0xE6:
                return cpu.and();
            // OR n: A = A OR n
            case 0xF6:
                return cpu.or();
            // XOR n: A = A XOR n
            case 0xEE:
                return cpu.xor();
            // CP n: Identical to A - n, but don't save the values
            case 0xFE:
                return cpu.cp();


            // INC n: increment the register n
            case 0x3C:
                return cpu.inc(CPURegister.Register.A);
            case 0x04:
                return cpu.inc(CPURegister.Register.B);
            case 0x0C:
                return cpu.inc(CPURegister.Register.C);
            case 0x14:
                return cpu.inc(CPURegister.Register.D);
            case 0x1C:
                return cpu.inc(CPURegister.Register.E);
            case 0x24:
                return cpu.inc(CPURegister.Register.H);
            case 0x2C:
                return cpu.inc(CPURegister.Register.L);
            case 0x34:
                return cpu.inc(CPURegister.Register.HL_ADDRESS);

            // DEC n: decrement the register n
            case 0x3D:
                return cpu.dec(CPURegister.Register.A);
            case 0x05:
                return cpu.dec(CPURegister.Register.B);
            case 0x0D:
                return cpu.dec(CPURegister.Register.C);
            case 0x15:
                return cpu.dec(CPURegister.Register.D);
            case 0x1D:
                return cpu.dec(CPURegister.Register.E);
            case 0x25:
                return cpu.dec(CPURegister.Register.H);
            case 0x2D:
                return cpu.dec(CPURegister.Register.L);
            case 0x35:
                return cpu.dec(CPURegister.Register.HL_ADDRESS);

            ///////////////////////////////////////////
            //  ALU OPERATIONS  -  16-bit operations //
            ///////////////////////////////////////////

            // ADD R -> HL: Add value in register R to HL
            case 0x09:
                return cpu.add(CPURegister.Register.HL, CPURegister.Register.BC, false);
            case 0x19:
                return cpu.add(CPURegister.Register.HL, CPURegister.Register.DE, false);
            case 0x29:
                return cpu.add(CPURegister.Register.HL, CPURegister.Register.HL, false);
            case 0x39:
                return cpu.add(CPURegister.Register.HL, CPURegister.Register.SP, false);

            // Add n -> SP: Add byte n to the SP register
            case 0xE8:
                return cpu.add(CPURegister.Register.SP, false);

            // INC RR: increment 16-bit register RR
            case 0x03:
                return cpu.inc(CPURegister.Register.BC);
            case 0x13:
                return cpu.inc(CPURegister.Register.DE);
            case 0x23:
                return cpu.inc(CPURegister.Register.HL);
            case 0x33:
                return cpu.inc(CPURegister.Register.SP);

            // DEC RR: decrement 16-bit register RR
            case 0x0B:
                return cpu.dec(CPURegister.Register.BC);
            case 0x1B:
                return cpu.dec(CPURegister.Register.DE);
            case 0x2B:
                return cpu.dec(CPURegister.Register.HL);
            case 0x3B:
                return cpu.dec(CPURegister.Register.SP);


            ////////////////////////////////
            //  Miscellaneous operations  //
            ////////////////////////////////

            // DAA: adjust the A register to account for binary operations
            // done on BCD (binary coded decimal) numbers
            case 0x27:
                return cpu.daa();

            // CPL: complement (flip the bits of) the A register
            case 0x2F:
                return cpu.complement();

            // CCF: complement the carry flag
            case 0x3F:
                return cpu.complementCarryFlag();

            // SCF: setBit carry flag
            case 0x37:
                return cpu.setCarryFlag();

            // NOP: no operation
            case 0x00:
                return cpu.noOperation();

            // HALT: power down return cpu.until interrupt occurs
            case 0x76:
                return cpu.halt();

            case 0x10:
                // STOP: halt CPU and LCD until button pressed
                return cpu.stop();

            // DI: disable interrupts until re-enabled
            case 0xF3:
                return cpu.disableInterrupts();

            // EI: enable interrupts after command after this one executed
            case 0xFB:
                return cpu.enableInterrupts();

            // RLCA: rotate A left, previous bit 7 becomes carry flag
            case 0x07:
                return cpu.rotateLeft(CPURegister.Register.A);

            // RLA: rotate A left through the carry flag
            case 0x17:
                return cpu.rotateLeftThroughCarry(CPURegister.Register.A);

            // RRCA: rotate A right, old bit 0 to carry flag.
            case 0x0F:
                return cpu.rotateRight(CPURegister.Register.A);

            // RRA: rotate A right through carry flag
            case 0x1F:
                return cpu.rotateRightThroughCarry(CPURegister.Register.A);

            // JP nn: jump to address nn
            case 0xC3:
                return cpu.jump();

            // JP cc,nn
            // Jump if z flag is false
            case 0xC2:
                return cpu.jumpZFlag(false);
            // Jump if z flag is true
            case 0xCA:
                return cpu.jumpZFlag(true);
            // Jump if C flag is false
            case 0xD2:
                return cpu.jumpCFlag(false);
            // Jump if C flag is true
            case 0xDA:
                return cpu.jumpCFlag(true);

            // JP (HL): jump to the address in HL
            case 0xE9:
                return cpu.jump(CPURegister.Register.HL);

            // JR n: Add n to current address and jump to it
            case 0x18:
                return cpu.jumpRelative();

            // JR cc,n: Jump to (current address + n) if flags are setBit as desired
            // Jump if z flag is false
            case 0x20:
                return cpu.jumpZFlagRelative(false);
            // Jump if z flag is true
            case 0x28:
                return cpu.jumpZFlagRelative(true);
            // Jump if C flag is false
            case 0x30:
                return cpu.jumpCFlagRelative(false);
            // Jump if C flag is true
            case 0x38:
                return cpu.jumpCFlagRelative(true);

            // CALL nn: Push address of next instruction onto stack and then
            // jump to address nn.
            case 0xCD:
                return cpu.call();
            // CALL cc,nn: Call address nn depending on CPU flags
            // z flag
            case 0xC4:
                return cpu.callZFlag(false);
            case 0xCC:
                return cpu.callZFlag(true);
            case 0xD4:
                return cpu.callCFlag(false);
            case 0xDC:
                return cpu.callCFlag(true);

            // RST n: Push present address onto stack, jump to address ($0000 + n)
            case 0xC7:
                return cpu.rest((byte) 0x0);
            case 0xCF:
                return cpu.rest((byte) 0x8);
            case 0xD7:
                return cpu.rest((byte) 0x10);
            case 0xDF:
                return cpu.rest((byte) 0x18);
            case 0xE7:
                return cpu.rest((byte) 0x20);
            case 0xEF:
                return cpu.rest((byte) 0x28);
            case 0xF7:
                return cpu.rest((byte) 0x30);
            case 0xFF:
                return cpu.rest((byte) 0x38);

            // RET: pop two bytes from the stack then jump to that address
            case 0xC9:
                return cpu.ret();

            // RET cc: return if flags are setBit as desired
            case 0xC0:
                return cpu.retZ(false);
            case 0xC8:
                return cpu.retZ(true);
            case 0xD0:
                return cpu.retC(false);
            case 0xD8:
                return cpu.retC(true);

            // RETI: Pop two bytes from the stack, jump to that address, and
            // then enable interrupts
            case 0xD9:
                return cpu.reti();


            // CB prefix- means the next operation will run the CB variant
            case 0xCB:
                // Run for 4 cycles regardless of the upcoming command:
                //      (this is b/c the CB command takes 4 cycles)
                cbPending = true;
                return cpu.noOperation();

            ////////////////////////////
            //  CB-prefixed commands  //
            ////////////////////////////

            // CB prefixed commands are an additional 256 commands available to
            // the Gameboy CPU. These commands occur after the CB command
            // finishes executing. Their opcodes are not actually prepended
            // like 0xCB37 (the opcode corresponding to 0xCB337 is 0x37), that
            // has been added as a way to differentiate them visually for ease
            // of reading

            // SWAP R: swap the upper and lower nibbles in register R
            case 0xCB37:
                return cpu.swap(CPURegister.Register.A);
            case 0xCB30:
                return cpu.swap(CPURegister.Register.B);
            case 0xCB31:
                return cpu.swap(CPURegister.Register.C);
            case 0xCB32:
                return cpu.swap(CPURegister.Register.D);
            case 0xCB33:
                return cpu.swap(CPURegister.Register.E);
            case 0xCB34:
                return cpu.swap(CPURegister.Register.H);
            case 0xCB35:
                return cpu.swap(CPURegister.Register.L);
            case 0xCB36:
                return cpu.swap(CPURegister.Register.HL_ADDRESS);

            // RLC n: rotate bits in n left. Old bit 7 to Carry flag.
            case 0xCB07:
                return cpu.rotateLeft(CPURegister.Register.A);
            case 0xCB00:
                return cpu.rotateLeft(CPURegister.Register.B);
            case 0xCB01:
                return cpu.rotateLeft(CPURegister.Register.C);
            case 0xCB02:
                return cpu.rotateLeft(CPURegister.Register.D);
            case 0xCB03:
                return cpu.rotateLeft(CPURegister.Register.E);
            case 0xCB04:
                return cpu.rotateLeft(CPURegister.Register.H);
            case 0xCB05:
                return cpu.rotateLeft(CPURegister.Register.L);
            case 0xCB06:
                return cpu.rotateLeft(CPURegister.Register.HL_ADDRESS);

            // RL n: rotate bits in n through carry flag.
            case 0xCB17:
                return cpu.rotateLeftThroughCarry(CPURegister.Register.A);
            case 0xCB10:
                return cpu.rotateLeftThroughCarry(CPURegister.Register.B);
            case 0xCB11:
                return cpu.rotateLeftThroughCarry(CPURegister.Register.C);
            case 0xCB12:
                return cpu.rotateLeftThroughCarry(CPURegister.Register.D);
            case 0xCB13:
                return cpu.rotateLeftThroughCarry(CPURegister.Register.E);
            case 0xCB14:
                return cpu.rotateLeftThroughCarry(CPURegister.Register.H);
            case 0xCB15:
                return cpu.rotateLeftThroughCarry(CPURegister.Register.L);
            case 0xCB16:
                return cpu.rotateLeftThroughCarry(CPURegister.Register.HL_ADDRESS);

            // RRC n: rotate bits in n right, old bit 0 to carry flag
            case 0xCB0F:
                return cpu.rotateRight(CPURegister.Register.A);
            case 0xCB08:
                return cpu.rotateRight(CPURegister.Register.B);
            case 0xCB09:
                return cpu.rotateRight(CPURegister.Register.C);
            case 0xCB0A:
                return cpu.rotateRight(CPURegister.Register.D);
            case 0xCB0B:
                return cpu.rotateRight(CPURegister.Register.E);
            case 0xCB0C:
                return cpu.rotateRight(CPURegister.Register.H);
            case 0xCB0D:
                return cpu.rotateRight(CPURegister.Register.L);
            case 0xCB0E:
                return cpu.rotateRight(CPURegister.Register.HL_ADDRESS);

            // RR n: rotate bits in n right through carry flag
            case 0xCB1F:
                return cpu.rotateRightThroughCarry(CPURegister.Register.A);
            case 0xCB18:
                return cpu.rotateRightThroughCarry(CPURegister.Register.B);
            case 0xCB19:
                return cpu.rotateRightThroughCarry(CPURegister.Register.C);
            case 0xCB1A:
                return cpu.rotateRightThroughCarry(CPURegister.Register.D);
            case 0xCB1B:
                return cpu.rotateRightThroughCarry(CPURegister.Register.E);
            case 0xCB1C:
                return cpu.rotateRightThroughCarry(CPURegister.Register.H);
            case 0xCB1D:
                return cpu.rotateRightThroughCarry(CPURegister.Register.L);
            case 0xCB1E:
                return cpu.rotateRightThroughCarry(CPURegister.Register.HL_ADDRESS);

            // SLA n: Shift n left into Carry. LSB of n setBit to 0.
            case 0xCB27:
                return cpu.shiftLeft(CPURegister.Register.A);
            case 0xCB20:
                return cpu.shiftLeft(CPURegister.Register.B);
            case 0xCB21:
                return cpu.shiftLeft(CPURegister.Register.C);
            case 0xCB22:
                return cpu.shiftLeft(CPURegister.Register.D);
            case 0xCB23:
                return cpu.shiftLeft(CPURegister.Register.E);
            case 0xCB24:
                return cpu.shiftLeft(CPURegister.Register.H);
            case 0xCB25:
                return cpu.shiftLeft(CPURegister.Register.L);
            case 0xCB26:
                return cpu.shiftLeft(CPURegister.Register.HL_ADDRESS);

            // SRA R: shifts R register to the right with bit 0 moved to
            // the carry flag and bit 0 retaining its original value
            case 0xCB2F:
                return cpu.shiftRight(CPURegister.Register.A, false);
            case 0xCB28:
                return cpu.shiftRight(CPURegister.Register.B, false);
            case 0xCB29:
                return cpu.shiftRight(CPURegister.Register.C, false);
            case 0xCB2A:
                return cpu.shiftRight(CPURegister.Register.D, false);
            case 0xCB2B:
                return cpu.shiftRight(CPURegister.Register.E, false);
            case 0xCB2C:
                return cpu.shiftRight(CPURegister.Register.H, false);
            case 0xCB2D:
                return cpu.shiftRight(CPURegister.Register.L, false);
            case 0xCB2E:
                return cpu.shiftRight(CPURegister.Register.HL_ADDRESS, false);

            // SRL R: shifts R register to the right with bit 0 moved to the
            // carry flag and bit 7 zeroed
            case 0xCB3F:
                return cpu.shiftRight(CPURegister.Register.A, true);
            case 0xCB38:
                return cpu.shiftRight(CPURegister.Register.B, true);
            case 0xCB39:
                return cpu.shiftRight(CPURegister.Register.C, true);
            case 0xCB3A:
                return cpu.shiftRight(CPURegister.Register.D, true);
            case 0xCB3B:
                return cpu.shiftRight(CPURegister.Register.E, true);
            case 0xCB3C:
                return cpu.shiftRight(CPURegister.Register.H, true);
            case 0xCB3D:
                return cpu.shiftRight(CPURegister.Register.L, true);
            case 0xCB3E:
                return cpu.shiftRight(CPURegister.Register.HL_ADDRESS, true);

            // BIT b, r: Test bit b in register r
            case 0xCB40:
            case 0xCB50:
            case 0xCB60:
            case 0xCB70:
            case 0xCB48:
            case 0xCB58:
            case 0xCB68:
            case 0xCB78:
                return cpu.checkBit(CPURegister.Register.B, bitValue);
            case 0xCB47:
            case 0xCB57:
            case 0xCB67:
            case 0xCB77:
            case 0xCB4F:
            case 0xCB5F:
            case 0xCB6F:
            case 0xCB7F:
                return cpu.checkBit(CPURegister.Register.A, bitValue);
            case 0xCB41:
            case 0xCB51:
            case 0xCB61:
            case 0xCB71:
            case 0xCB49:
            case 0xCB59:
            case 0xCB69:
            case 0xCB79:
                return cpu.checkBit(CPURegister.Register.C, bitValue);
            case 0xCB42:
            case 0xCB52:
            case 0xCB62:
            case 0xCB72:
            case 0xCB4A:
            case 0xCB5A:
            case 0xCB6A:
            case 0xCB7A:
                return cpu.checkBit(CPURegister.Register.D, bitValue);
            case 0xCB43:
            case 0xCB53:
            case 0xCB63:
            case 0xCB73:
            case 0xCB4B:
            case 0xCB5B:
            case 0xCB6B:
            case 0xCB7B:
                return cpu.checkBit(CPURegister.Register.E, bitValue);
            case 0xCB44:
            case 0xCB54:
            case 0xCB64:
            case 0xCB74:
            case 0xCB4C:
            case 0xCB5C:
            case 0xCB6C:
            case 0xCB7C:
                return cpu.checkBit(CPURegister.Register.H, bitValue);
            case 0xCB45:
            case 0xCB55:
            case 0xCB65:
            case 0xCB75:
            case 0xCB4D:
            case 0xCB5D:
            case 0xCB6D:
            case 0xCB7D:
                return cpu.checkBit(CPURegister.Register.L, bitValue);
            case 0xCB46:
            case 0xCB56:
            case 0xCB66:
            case 0xCB76:
            case 0xCB4E:
            case 0xCB5E:
            case 0xCB6E:
            case 0xCB7E:
                return cpu.checkBit(CPURegister.Register.HL_ADDRESS, bitValue);

            // RES b,r: Reset (setBit false) bit b in register R
            case 0xCBC0:
            case 0xCBD0:
            case 0xCBE0:
            case 0xCBF0:
            case 0xCBC8:
            case 0xCBD8:
            case 0xCBE8:
            case 0xCBF8:
                return cpu.setBit(CPURegister.Register.B, bitValue, false);
            case 0xCBC7:
            case 0xCBD7:
            case 0xCBE7:
            case 0xCBF7:
            case 0xCBCF:
            case 0xCBDF:
            case 0xCBEF:
            case 0xCBFF:
                return cpu.setBit(CPURegister.Register.A, bitValue, false);
            case 0xCBC1:
            case 0xCBD1:
            case 0xCBE1:
            case 0xCBF1:
            case 0xCBC9:
            case 0xCBD9:
            case 0xCBE9:
            case 0xCBF9:
                return cpu.setBit(CPURegister.Register.C, bitValue, false);
            case 0xCBC2:
            case 0xCBD2:
            case 0xCBE2:
            case 0xCBF2:
            case 0xCBCA:
            case 0xCBDA:
            case 0xCBEA:
            case 0xCBFA:
                return cpu.setBit(CPURegister.Register.D, bitValue, false);
            case 0xCBC3:
            case 0xCBD3:
            case 0xCBE3:
            case 0xCBF3:
            case 0xCBCB:
            case 0xCBDB:
            case 0xCBEB:
            case 0xCBFB:
                return cpu.setBit(CPURegister.Register.E, bitValue, false);
            case 0xCBC4:
            case 0xCBD4:
            case 0xCBE4:
            case 0xCBF4:
            case 0xCBCC:
            case 0xCBDC:
            case 0xCBEC:
            case 0xCBFC:
                return cpu.setBit(CPURegister.Register.H, bitValue, false);
            case 0xCBC5:
            case 0xCBD5:
            case 0xCBE5:
            case 0xCBF5:
            case 0xCBCD:
            case 0xCBDD:
            case 0xCBED:
            case 0xCBFD:
                return cpu.setBit(CPURegister.Register.L, bitValue, false);
            case 0xCBC6:
            case 0xCBD6:
            case 0xCBE6:
            case 0xCBF6:
            case 0xCBCE:
            case 0xCBDE:
            case 0xCBEE:
            case 0xCBFE:
                return cpu.setBit(CPURegister.Register.HL_ADDRESS, bitValue, false);

            // SET b,r: Set bit b in register R
            case 0xCB80:
            case 0xCB90:
            case 0xCBA0:
            case 0xCBB0:
            case 0xCB88:
            case 0xCB98:
            case 0xCBA8:
            case 0xCBB8:
                return cpu.setBit(CPURegister.Register.B, bitValue, true);
            case 0xCB87:
            case 0xCB97:
            case 0xCBA7:
            case 0xCBB7:
            case 0xCB8F:
            case 0xCB9F:
            case 0xCBAF:
            case 0xCBBF:
                return cpu.setBit(CPURegister.Register.A, bitValue, true);
            case 0xCB81:
            case 0xCB91:
            case 0xCBA1:
            case 0xCBB1:
            case 0xCB89:
            case 0xCB99:
            case 0xCBA9:
            case 0xCBB9:
                return cpu.setBit(CPURegister.Register.C, bitValue, true);
            case 0xCB82:
            case 0xCB92:
            case 0xCBA2:
            case 0xCBB2:
            case 0xCB8A:
            case 0xCB9A:
            case 0xCBAA:
            case 0xCBBA:
                return cpu.setBit(CPURegister.Register.D, bitValue, true);
            case 0xCB83:
            case 0xCB93:
            case 0xCBA3:
            case 0xCBB3:
            case 0xCB8B:
            case 0xCB9B:
            case 0xCBAB:
            case 0xCBBB:
                return cpu.setBit(CPURegister.Register.E, bitValue, true);
            case 0xCB84:
            case 0xCB94:
            case 0xCBA4:
            case 0xCBB4:
            case 0xCB8C:
            case 0xCB9C:
            case 0xCBAC:
            case 0xCBBC:
                return cpu.setBit(CPURegister.Register.H, bitValue, true);
            case 0xCB85:
            case 0xCB95:
            case 0xCBA5:
            case 0xCBB5:
            case 0xCB8D:
            case 0xCB9D:
            case 0xCBAD:
            case 0xCBBD:
                return cpu.setBit(CPURegister.Register.L, bitValue, true);
            case 0xCB86:
            case 0xCB96:
            case 0xCBA6:
            case 0xCBB6:
            case 0xCB8E:
            case 0xCB9E:
            case 0xCBAE:
            case 0xCBBE:
                return cpu.setBit(CPURegister.Register.HL_ADDRESS, bitValue, true);

            default:
                throw new UnknownOperationException(String.format("%02X", unsignedOpCode));

        } // CB command switch
    } // CB if statement
} // parse method

package mattpvaughn.io.github.emulator.cpu;

/* Tests for methods in the CPU class. Each test should ensure that functions
 * provide consistent behavior by CPU operations on each of the following:
 *      Memory
 *      Register values
 *      Cycles
 *      Bits consumed
 *      Flags set
 *
 *  Note that these tests are not designed to be exhaustive, as the very
 *  comprehensive freeware Blargg's test suite exists for Gameboy CPU testing.
 *  These tests were designed to help aid development and provided a quick test
 *  suite to prevent regressions.
 */

import mattpvaughn.io.github.emulator.Memory;
import org.junit.Test;

import static mattpvaughn.io.github.emulator.InterruptManager.InterruptState.*;
import static org.junit.Assert.*;

public class CPUTest {

    private static final int arbitraryMemoryAddress = 0xCDDD;
    private static final byte[] splitAddress = Util.splitShortToBytes(Util.shortenInt(arbitraryMemoryAddress));
    // 0x01000001 in binary
    private static final byte arbitraryByte = (byte) 65;

    // 0x00010110 in binary
    private static final byte arbitraryByte2 = (byte) 22;


    // Test an example write from the A register to the B register
    @Test
    public void testWriteRegisterToRegisterBasic() {
        // Set up default values in Registers
        CPURegister cpuRegister = new CPURegister();
        cpuRegister.setRegister(CPURegister.Register.A, arbitraryByte);
        cpuRegister.setRegister(CPURegister.Register.B, (byte) 0);

        // Initalize a CPU (note: no need to attach memory here)
        CPU cpu = new CPU.Builder().cpuRegister(cpuRegister).build();

        // Write from register A to register B
        int cycles = cpu.writeRegisterToRegister(CPURegister.Register.A, CPURegister.Register.B);

        // Ensure that register B now holds whatever value was held in register A
        assertEquals(arbitraryByte, cpuRegister.get8BitRegisterValue(CPURegister.Register.B));

        // Ensure that a normal 8-checkBit register write takes 4 cycles
        assertEquals(4, cycles);
    }

    // Test an example write from the A register to the address stored in the
    // HL register using meothd writeRegisterToRegister
    @Test
    public void testWrite8BitRegisterToHLAddress() {
        CPURegister cpuRegister = new CPURegister();

        // Store an arbitrary value in register A
        cpuRegister.setRegister(CPURegister.Register.A, arbitraryByte);

        // Store an arbitrary memory address in register HL
        cpuRegister.setRegister(CPURegister.Register.HL, splitAddress[0], splitAddress[1]);

        // Initialize the memory with zero setBit at the address currently stored in HL
        Memory memory = new Memory();
        memory.writeByte(Util.unsignedShortToInt(
                Util.shortenInt(arbitraryMemoryAddress)), (byte) 0);

        // Initalize the CPU
        CPU cpu = new CPU.Builder().cpuRegister(cpuRegister).memory(memory).build();

        // Attempt to write from whatever is stored at the A register the address stored in HL
        int cycles = cpu.writeRegisterToRegister(CPURegister.Register.A,
                CPURegister.Register.HL_ADDRESS);

        // Ensure that the address stored in the HL register has been written to
        assertEquals(arbitraryByte, memory.readByte(Util.unsignedShortToInt(cpuRegister.get16BitRegisterValue(CPURegister.Register.HL))));

        // Ensure that a normal 8-checkBit register write takes 4 cycles
        assertEquals(cycles, 8);
    }

    // Test an example write from some address saved in the HL register into
    // the A register
    @Test
    public void testWriteFromHLAddressToRegister() {
        // Set up default values in Registers
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();

        // Split memory address, store in HL register
        cpuRegister.setRegister(CPURegister.Register.HL, splitAddress[0], splitAddress[1]);

        // Set up the CPU and memory
        CPU cpu = new CPU.Builder().memory(memory).cpuRegister(cpuRegister).build();

        // Set the value of the address of the HL register to an arbitrary byte
        memory.writeByte(Util.unsignedShortToInt(cpuRegister.get16BitRegisterValue(CPURegister.Register.HL)),
                arbitraryByte);

        // Now test writing from that address into the A register
        cpu.writeRegisterToRegister(CPURegister.Register.HL, CPURegister.Register.A);

        // Test that A == 234
        assertEquals(arbitraryByte, cpuRegister.A);
    }

    // Test writeByteToRegister(R, RR) in the CPU
    @Test
    public void testBasicWriteMemoryToRegister() {
        // Basic CPU setup
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().memory(memory).cpuRegister(cpuRegister).build();

        // Zero the A register
        cpuRegister.setRegister(CPURegister.Register.A, (byte) 0);

        // Store an address in the DE 16-checkBit register
        cpuRegister.setRegister(CPURegister.Register.DE, splitAddress[0], splitAddress[1]);

        // Write an arbitrary byte to that memory address
        memory.writeByte(arbitraryMemoryAddress, arbitraryByte);

        // Now write from that memory to the A register
        cpu.writeMemoryToRegister(CPURegister.Register.A, CPURegister.Register.DE);

        // Test that A == 234
        assertEquals(arbitraryByte, cpuRegister.A);
    }

    // Write value from ($FF00 + register C) to A
    @Test
    public void testWriteCToABasic() {
        // Basic CPU setup
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().memory(memory).cpuRegister(cpuRegister).build();

        // Set some 8-checkBit value to register C- this will end up being the end
        // of an address
        cpuRegister.setRegister(CPURegister.Register.C, arbitraryByte);
        // Zero A
        cpuRegister.setRegister(CPURegister.Register.A, (byte) 0);

        // Set the value at address ($FF00 + register C) to some arbitrary number
        int address = cpuRegister.get8BitRegisterValue(CPURegister.Register.C) + 0xFF00;
        memory.writeByte(address, arbitraryByte2);

        // Write value at address ($FF00 + register C) into A
        cpu.writeCToA();

        // Check that the value in register A has been updated
        assertEquals(arbitraryByte2, cpuRegister.get8BitRegisterValue(CPURegister.Register.A));
    }

    // Write value from register A to address ($FF00 + register C)
    @Test
    public void testWriteAToCBasic() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().cpuRegister(cpuRegister).memory(memory).build();

        // Set some arbitrary value to register A
        cpuRegister.setRegister(CPURegister.Register.A, arbitraryByte);

        // Write A into (0xFF00 + C)
        cpu.writeAToC();

        // Get the address ($FF00 + register C)
        int address = 0xFF00 + cpuRegister.get8BitRegisterValue(CPURegister.Register.C);

        assertEquals(arbitraryByte, memory.readByte(address));
    }

    @Test
    public void testWriteMemoryToRegisterCrementBasic() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().memory(memory).cpuRegister(cpuRegister).build();

        // Set the value at the DE register to be some arbitrary address
        cpuRegister.setRegister(CPURegister.Register.HL, splitAddress[0],
                splitAddress[1]);
        cpuRegister.setRegister(CPURegister.Register.A, (byte) 0);

        // Set the value at (DE) to be some arbitrary byte
        memory.writeByte(arbitraryMemoryAddress, arbitraryByte);

        // Write from the address stored in DE to A, incrementing DE
        cpu.writeMemoryToRegisterCrement(CPURegister.Register.A, 1);

        // Check that increment is working
        assertEquals(arbitraryMemoryAddress + 1,
                Util.unsignedShortToInt(
                        cpuRegister.get16BitRegisterValue(CPURegister.Register.HL)));

        // Check that the value at that address is written to A
        assertEquals(arbitraryByte, cpuRegister.A);

    }

    @Test
    public void testWriteRegisterToMemoryCrement() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().memory(memory).cpuRegister(cpuRegister).build();

        // Set the value in the DE register to be some arbitrary address
        cpuRegister.setRegister(CPURegister.Register.HL, splitAddress[0], splitAddress[1]);

        // Set the value in A to be some arbitrary byte
        cpuRegister.setRegister(CPURegister.Register.A, arbitraryByte);

        // Write from register A to the memory address stored in HL, incrementing HL
        cpu.writeFromRegisterToMemory(CPURegister.Register.A, 1);

        // Ensure the value was written from A to (HL)
        assertEquals(arbitraryByte, memory.readByte(arbitraryMemoryAddress));

        // Ensure DE was incremented
        assertEquals(arbitraryMemoryAddress + 1, Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(CPURegister.Register.HL)));
    }

    @Test
    public void testWriteRegisterToMemoryByteBasic() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder().memory(memory).programCounter(pc).cpuRegister(cpuRegister).build();

        // Set register A to have an arbitrary value
        cpuRegister.setRegister(CPURegister.Register.A, arbitraryByte);

        // Loads some bytes to be loaded next from ROM
        pc.reset();
        memory.loadBytesToRom(new byte[]{arbitraryByte2}, 0);

        // Address to be written to will be the following
        int address = 0xFF00 + arbitraryByte2;

        // Write from register A into memory address (0xFF00 + n), where n is
        // the next byte in ROM
        cpu.writeRegisterToMemoryByte(CPURegister.Register.A);

        // Ensure that address (0xFF00 + n) now contains the byte from register A
        assertEquals(arbitraryByte, memory.readByte(address));
    }

    @Test
    public void testWriteRegisterToMemoryBasic() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder().memory(memory).programCounter(pc).cpuRegister(cpuRegister).build();

        // Put an arbitrary value in A
        cpuRegister.setRegister(CPURegister.Register.A, arbitraryByte);

        // Write the next two bytes in ROM to be an address
        pc.reset();
        memory.loadBytesToRom(new byte[]{splitAddress[1], splitAddress[0]}, 0);

        int cycles = cpu.writeRegisterToMemory(CPURegister.Register.A);

        // Ensure that the value we put in A appears in the address represented
        // by the next 2 bytes in ROM
        assertEquals(arbitraryByte, memory.readByte(arbitraryMemoryAddress));

        assertEquals(16, cycles);
    }

    @Test
    public void testWriteByteToRegisterBasic() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder().memory(memory).programCounter(pc).cpuRegister(cpuRegister).build();

        // Put arbitrary byte in the ROM like it's the next byte in rom
        pc.reset();
        memory.loadBytesToRom(new byte[]{arbitraryByte}, 0);

        // Write that value to register A
        int cycles = cpu.writeByteToRegister(CPURegister.Register.A);

        assertEquals(arbitraryByte, cpuRegister.get8BitRegisterValue(CPURegister.Register.A));

        assertEquals(8, cycles);
    }

    @Test
    public void testWriteByteToRegisterHL() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder().memory(memory).programCounter(pc).cpuRegister(cpuRegister).build();

        // Set up HL so it points somewhere in memory
        cpuRegister.setRegister(CPURegister.Register.HL, splitAddress[0], splitAddress[1]);

        // Put arbitrary byte in the ROM like it's the next byte in rom
        pc.reset();
        memory.loadBytesToRom(new byte[]{arbitraryByte}, 0);

        // Write that value to register A
        int cycles = cpu.writeByteToRegister(CPURegister.Register.HL);

        assertEquals(arbitraryByte,
                memory.readByte(
                        Util.unsignedShortToInt(
                                cpuRegister.get16BitRegisterValue(CPURegister.Register.HL))));

        assertEquals(12, cycles);
    }

    @Test
    public void testWriteMemoryByteToRegisterBasic() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder().memory(memory).programCounter(pc).cpuRegister(cpuRegister).build();

        // Put arbitrary byte in the next readable location in the ROM
        pc.reset();
        memory.loadBytesToRom(new byte[]{arbitraryByte2}, 0);

        // Get address where we will write from
        int address = 0xFF00 + arbitraryByte2;

        // Write an arbitrary byte to (0xFF00 + n)
        memory.writeByte(address, arbitraryByte);

        // Test the CPU's impl. of writeMemoryByteToRegister using register A
        int cycles = cpu.writeMemoryByteToRegister(CPURegister.Register.A);

        assertEquals(arbitraryByte, cpuRegister.get8BitRegisterValue(CPURegister.Register.A));
        assertEquals(12, cycles);
    }

    @Test
    public void testWrite16BitValueToRegisterBasic() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder()
                .memory(memory)
                .programCounter(pc)
                .cpuRegister(cpuRegister)
                .build();

        // Set the next 2 bytes in ROM
        memory.loadBytesToRom(new byte[]{arbitraryByte2, arbitraryByte}, 0);

        // Run on register DE
        int cycles = cpu.write16BitValueToRegister(CPURegister.Register.DE);

        assertEquals(arbitraryByte, cpuRegister.D);
        assertEquals(arbitraryByte2, cpuRegister.E);
        assertEquals(12, cycles);
    }

    @Test
    public void testWrite8BitValueToRegisterBasic() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder()
                .memory(memory)
                .programCounter(pc)
                .cpuRegister(cpuRegister)
                .build();

        // Set the next 2 bytes in ROM
        memory.loadBytesToRom(new byte[]{arbitraryByte}, 0);

        // Run on register DE
        int cycles = cpu.write8BitValueToRegister(CPURegister.Register.A);

        assertEquals(arbitraryByte, cpuRegister.A);
        assertEquals(8, cycles);
    }

    @Test
    public void testWriteSPtoHL() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder().memory(memory).programCounter(pc).cpuRegister(cpuRegister).build();

        // Set an arbitrary default value for SP
        cpuRegister.set16BitRegister(CPURegister.Register.SP, arbitraryByte);

        // Set the next byte in ROM to be a different arbitrary value
        pc.reset();
        memory.loadBytesToRom(new byte[]{arbitraryByte2}, 0);

        cpu.writeSP8BitToHL();

        assertEquals(arbitraryByte + arbitraryByte2,
                cpuRegister.get16BitRegisterValue(CPURegister.Register.HL));
    }

    @Test
    public void testPushBasic() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().memory(memory).cpuRegister(cpuRegister).build();

        // Put an arbitrary 16-checkBit value in DE
        cpuRegister.setRegister(CPURegister.Register.DE, arbitraryByte, arbitraryByte2);

        // Set SP to some arbitrary memory address
        cpuRegister.setRegister(CPURegister.Register.SP, splitAddress[0], splitAddress[1]);

        // Push from DE to (SP)
        int cycles = cpu.push(CPURegister.Register.DE);

        // Check that SP has been decremented properly
        assertEquals(arbitraryMemoryAddress - 2,
                Util.unsignedShortToInt(cpuRegister.get16BitRegisterValue(CPURegister.Register.SP)));

        // Check that bytes have been pushed to correct locations
        assertEquals(arbitraryByte, memory.readByte(arbitraryMemoryAddress - 1));
        assertEquals(arbitraryByte2, memory.readByte(arbitraryMemoryAddress - 2));

        // Check correct cycle count
        assertEquals(16, cycles);
    }

    @Test
    public void testPopBasic() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().memory(memory).cpuRegister(cpuRegister).build();

        // Push two bytes onto the stack first...
        cpuRegister.setRegister(CPURegister.Register.SP, splitAddress[0], splitAddress[1]);
        memory.writeByte(arbitraryMemoryAddress + 0, arbitraryByte);
        memory.writeByte(arbitraryMemoryAddress + 1, arbitraryByte2);

        // Pop those bytes onto DE
        int cycles = cpu.pop(CPURegister.Register.DE);

        // Check that the values in DE are correct
        assertEquals(arbitraryByte, cpuRegister.D);
        assertEquals(arbitraryByte2, cpuRegister.E);

        // Check SP incremented correctly
        assertEquals(arbitraryMemoryAddress + 2, Util.unsignedShortToInt(cpuRegister.SP));

        // Check correct cycle count
        assertEquals(12, cycles);
    }

    // Make sure push and pop work together i.e. if you push two bytes onto the
    // stack, calling pop removes those same bytes
    @Test
    public void testPushPop() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().memory(memory).cpuRegister(cpuRegister).build();

        // Push two bytes onto the stack first...
        cpuRegister.setRegister(CPURegister.Register.SP, splitAddress[0], splitAddress[1]);
        cpuRegister.set16BitRegister(CPURegister.Register.DE, (short) 0b00001111);
        cpu.push(CPURegister.Register.DE);

        // Clear DE
        cpuRegister.set16BitRegister(CPURegister.Register.DE, (short) 0);

        // Pop those bytes back off the stack onto DE
        int cycles = cpu.pop(CPURegister.Register.DE);

        System.out.println(cpuRegister.get16BitRegisterValue(CPURegister.Register.DE));

        // Check that the values in DE are correct
        assertEquals(0b0000, cpuRegister.D);
        assertEquals((byte) 0b1111, cpuRegister.E);

        // Check SP is what it was originally, as it incremented then decremented
        assertEquals(arbitraryMemoryAddress,
                Util.unsignedShortToInt(cpuRegister.SP));

        // Check correct cycle count
        assertEquals(12, cycles);
    }

    @Test
    public void testAddBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Set values in A and R to be added
        cpuRegister.A = arbitraryByte;
        cpuRegister.B = arbitraryByte2;

        // Add, don't worry about considering carries
        int cycles = cpu.add(CPURegister.Register.B, false);

        // Test that the sum is correct
        assertEquals(arbitraryByte + arbitraryByte2, cpuRegister.A);

        // Check flags
        assertFalse(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h); // no half carries b/w these numbers
        assertFalse(flagRegister.c); // no carries b/w these numbers

        // Check cycles
        assertEquals(4, cycles);
    }

    // Test that the CPU's add function handles carries
    @Test
    public void testAddCarry() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Set values in A and R to be added
        byte bigByte = (byte) 0b11000000;
        cpuRegister.A = bigByte;
        cpuRegister.B = bigByte;

        int cycles = cpu.add(CPURegister.Register.B, false);

        // Test that the sum is correct
        assertEquals((byte) (bigByte + bigByte), cpuRegister.A);

        System.out.println(bigByte);

        // Check flags
        assertEquals(((byte) (bigByte + bigByte)) == 0, flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertTrue(flagRegister.c);

        // Check cycles
        assertEquals(4, cycles);
    }

    // Test that the CPU's add function handles half carries
    @Test
    public void testAddHalfCarry() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Set values in A and R to be added
        byte bigByte = 0b00001000;
        cpuRegister.A = bigByte;
        cpuRegister.B = bigByte;

        int cycles = cpu.add(CPURegister.Register.B, false);

        // Test that the sum is correct
        assertEquals((byte) (bigByte + bigByte), cpuRegister.A);

        // Check flags
        assertEquals(((bigByte + bigByte) == 0), flagRegister.z);
        assertFalse(flagRegister.n);
        assertTrue(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testAddHL() {
        fail();
    }

    // Make sure add does not carry when it is very close to having to carry
    @Test
    public void testAddNoCarry() {
        fail();
    }

    @Test
    public void testAddLiteral() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        ProgramCounter pc = new ProgramCounter();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .programCounter(pc)
                .flagRegister(flagRegister)
                .build();

        // Put a byte into ROM to be added to register A
        memory.loadBytesToRom(new byte[]{arbitraryByte2}, 0);

        // Give register A a default value
        cpuRegister.A = arbitraryByte;

        int cycles = cpu.add(false);

        // Test that the sum is correct
        assertEquals((byte) (arbitraryByte + arbitraryByte2), cpuRegister.A);

        // Check flags
        assertEquals(((arbitraryByte + arbitraryByte2) == 0), flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(8, cycles);
    }


    @Test
    public void testSubBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Set values in A and R to be added which will not result in borrows
        byte biggerByte = 0b01010101;
        byte smallerByte = 0b00000001;
        cpuRegister.A = biggerByte;
        cpuRegister.B = smallerByte;

        int cycles = cpu.sub(CPURegister.Register.B, false);

        // Test that the sum is correct
        assertEquals((byte) (biggerByte - smallerByte), cpuRegister.A);

        // Check flags
        assertEquals(((biggerByte + smallerByte) == 0), flagRegister.z);
        assertTrue(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testSubBorrow() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Set it up so we have 0 - 1. This will cause a borrow and a half
        // borrow
        byte zeroByte = 0b00000000;
        byte nonZeroByte = 0b00000001;
        cpuRegister.A = zeroByte;
        cpuRegister.B = nonZeroByte;

        int cycles = cpu.sub(CPURegister.Register.B, false);

        // Test that the sum is correct
        assertEquals((byte) (zeroByte - nonZeroByte), cpuRegister.A);

        // Check flags
        assertEquals(((zeroByte - nonZeroByte) == 0), flagRegister.z);
        assertTrue(flagRegister.n);
        assertTrue(flagRegister.h);
        assertTrue(flagRegister.c);

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testSubHL() {
        fail();
    }

    @Test
    public void testAndBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().flagRegister(flagRegister).cpuRegister(cpuRegister).build();

        // Set arbitrary values in the A and B register
        cpuRegister.A = arbitraryByte;
        cpuRegister.B = arbitraryByte2;

        // A AND B
        int cycles = cpu.and(CPURegister.Register.B);

        // Check that operation suceeded
        assertEquals(arbitraryByte & arbitraryByte2, cpuRegister.A);

        // Manually did it as well just to be careful
        assertEquals(0x0000000, cpuRegister.A);

        // Check flags:
        assertTrue(flagRegister.z);
        assertFalse(flagRegister.n);
        assertTrue(flagRegister.h);
        assertFalse(flagRegister.c);

        assertEquals(4, cycles);
    }

    @Test
    public void testOrBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().flagRegister(flagRegister).cpuRegister(cpuRegister).build();

        // Set arbitrary values in the A and B registers
        cpuRegister.A = arbitraryByte;
        cpuRegister.B = arbitraryByte2;

        // A OR B
        int cycles = cpu.or(CPURegister.Register.B);

        // Check that operation succeeded
        assertEquals(arbitraryByte | arbitraryByte2, cpuRegister.A);

        // Manual check
        assertEquals(0b01010111, cpuRegister.A);

        // Check Flags
        assertFalse(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        assertEquals(4, cycles);
    }

    @Test
    public void testXorBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().flagRegister(flagRegister).cpuRegister(cpuRegister).build();

        // Set arbitrary values in the A and B registers, don't use the same
        // ones that we usually use b/c they would XOR to 0, which would be sad
        byte value1 = 0b01010101;
        byte value2 = 0b00001111;
        cpuRegister.A = value1;
        cpuRegister.B = value2;

        // A OR B
        int cycles = cpu.xor(CPURegister.Register.B);

        // Check that operation succeeded
        assertEquals(value1 ^ value2, cpuRegister.A);

        // Manual check
        assertEquals(0b01011010, cpuRegister.A);

        // Check Flags
        assertFalse(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        assertEquals(4, cycles);
    }

    @Test
    public void testXorHL() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder()
                .flagRegister(flagRegister)
                .memory(memory)
                .cpuRegister(cpuRegister)
                .build();

        // Set arbitrary values in the A and B registers, don't use the same
        // ones that we usually use b/c they would XOR to 0, which would be sad
        byte value1 = 0b01010101;
        byte value2 = 0b00001111;
        cpuRegister.A = value1;
        cpuRegister.setRegister(CPURegister.Register.HL, splitAddress[0], splitAddress[1]);

        memory.writeByte(arbitraryMemoryAddress, value2);

        // A OR B
        int cycles = cpu.xor(CPURegister.Register.HL_ADDRESS);

        // Check that operation succeeded
        assertEquals(value1 ^ value2, cpuRegister.A);

        // Manual check
        assertEquals(0b01011010, cpuRegister.A);

        // Check Flags
        assertFalse(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        assertEquals(8, cycles);
    }

    @Test
    public void testAndLiteral() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .memory(memory)
                .flagRegister(flagRegister)
                .cpuRegister(cpuRegister)
                .build();

        // Put an arbitrary value in register A
        cpuRegister.A = arbitraryByte;

        // Make the next byte in ROM a different arbitrary value
        memory.loadBytesToRom(new byte[]{arbitraryByte2}, 0);

        // Call and(), expect A = A AND rom[0] = arbitraryByte & arbitraryByte2
        int cycles = cpu.and();

        // Ensure register A contains the correct value
        assertEquals(arbitraryByte & arbitraryByte2, cpuRegister.A);

        // Check flags:
        assertTrue(flagRegister.z);
        assertFalse(flagRegister.n);
        assertTrue(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(8, cycles);

    }

    @Test
    public void testOrLiteral() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .memory(memory)
                .cpuRegister(cpuRegister)
                .build();

        // Put an arbitrary value in register A
        cpuRegister.A = arbitraryByte;

        // Make the next byte in ROM a different arbitrary value
        memory.loadBytesToRom(new byte[]{arbitraryByte2}, 0);

        // Call and(), expect A = A AND rom[0] = arbitraryByte & arbitraryByte2
        int cycles = cpu.or();

        // Ensure register A contains the correct value
        assertEquals(arbitraryByte | arbitraryByte2, cpuRegister.A);

        // Check flags:
        assertFalse(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(8, cycles);
    }

    @Test
    public void testXorLiteral() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .memory(memory)
                .cpuRegister(cpuRegister)
                .build();

        // Put an arbitrary value in register A
        cpuRegister.A = arbitraryByte;

        // Make the next byte in ROM a different arbitrary value
        memory.loadBytesToRom(new byte[]{arbitraryByte2}, 0);

        // Call and(), expect A = A AND rom[0] = arbitraryByte & arbitraryByte2
        int cycles = cpu.xor();

        // Ensure register A contains the correct value
        assertEquals(arbitraryByte ^ arbitraryByte2, cpuRegister.A);

        // Check flags:
        assertFalse(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(8, cycles);
    }

    @Test
    public void testIncBasic() {
        // Use a normal 8-bit register
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Set up register
        cpuRegister.B = 0b00001111;

        int cycles = cpu.inc(CPURegister.Register.B);

        // Check that increment was successful
        assertEquals(0b00001111 + 1, cpuRegister.B);

        // Check flags
        assertEquals(arbitraryByte + 1 == 0, flagRegister.z);
        assertFalse(flagRegister.n);
        assertTrue(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testInc16Bit() {
        // Use a normal 8-bit register
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Set up register
        cpuRegister.setRegister(CPURegister.Register.DE, splitAddress[0], splitAddress[1]);

        // Increment 16-bit register
        int cycles = cpu.inc(CPURegister.Register.DE);

        // Check that increment was successful
        assertEquals(arbitraryMemoryAddress + 1, Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(CPURegister.Register.DE)));

        // Check flags
        assertEquals(arbitraryMemoryAddress + 1 == 0, flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(8, cycles);
    }

    @Test
    public void testIncHLPointer() {
        // Use a normal 8-bit register
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .flagRegister(flagRegister)
                .build();

        // Set up HL register to hold address, put arbitrary byte at that addr
        cpuRegister.setRegister(CPURegister.Register.HL, splitAddress[0], splitAddress[1]);
        memory.writeByte(arbitraryMemoryAddress, arbitraryByte);

        // Increment 16-bit register
        int cycles = cpu.inc(CPURegister.Register.HL_ADDRESS);

        // Check that increment was successful
        assertEquals(arbitraryByte + 1, memory.readByte(arbitraryMemoryAddress));

        // Check flags
        assertEquals(arbitraryByte2 + 1 == 0, flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(12, cycles);
    }

    @Test
    public void testDecBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Put some value in an 8-bit register (the value will not cause a
        // borrow or half-borrow)
        byte testByte = 0b00000011;
        cpuRegister.A = testByte;
        boolean oldC = flagRegister.c;

        // Decrement the value in the A register
        int cycles = cpu.dec(CPURegister.Register.A);

        // Check that the A register decremented
        assertEquals(testByte - 1, cpuRegister.A);

        // Check flags
        assertEquals(testByte - 1 == 0, flagRegister.z);
        assertTrue(flagRegister.n);
        assertFalse(flagRegister.h);
        assertEquals(oldC, flagRegister.c); // Should not change

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testDecHalfBorrow() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Put some value in an 8-bit register (the value will not cause a
        // borrow or half-borrow)
        byte testByte = 0b00001000;
        cpuRegister.A = testByte;
        boolean oldC = flagRegister.c;

        // Decrement the value in the A register
        int cycles = cpu.dec(CPURegister.Register.A);

        // Check that the A register decremented
        assertEquals(testByte - 1, cpuRegister.A);

        // Check flags
        assertEquals(testByte - 1 == 0, flagRegister.z);
        assertTrue(flagRegister.n);
        assertFalse(flagRegister.h);
        assertEquals(oldC, flagRegister.c); // Should not change

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testDec16Bit() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Put some value in a 16-bit register
        byte testByte = 0x000F;
        cpuRegister.set16BitRegister(CPURegister.Register.DE, testByte);
        boolean oldC = flagRegister.c;

        // Decrement the value in the DE register
        int cycles = cpu.dec(CPURegister.Register.DE);

        // Check that the DE register decremented
        assertEquals(testByte - 1, cpuRegister.get16BitRegisterValue(CPURegister.Register.DE));

        // Don't check flags-- they aren't affected byte 16-bit dec

        // Check cycles
        assertEquals(8, cycles);
    }

    // Test decrementing (HL)
    @Test
    public void testDecHLPointer() {
        // Use a normal 8-bit register
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .flagRegister(flagRegister)
                .build();

        // Set up HL register to hold address, put arbitrary byte at that addr
        cpuRegister.setRegister(CPURegister.Register.HL, splitAddress[0], splitAddress[1]);
        memory.writeByte(arbitraryMemoryAddress, arbitraryByte);
        boolean oldC = flagRegister.c;

        // Increment 16-bit register
        int cycles = cpu.dec(CPURegister.Register.HL_ADDRESS);

        // Check that increment was successful
        assertEquals(arbitraryByte - 1, memory.readByte(arbitraryMemoryAddress));

        // Check flags
        assertEquals(arbitraryByte2 - 1 == 0, flagRegister.z);
        assertTrue(flagRegister.n);
        assertFalse(flagRegister.h);
        assertEquals(oldC, flagRegister.c); // Ensure c isn't changed by dec

        // Check cycles
        assertEquals(12, cycles);
    }

    @Test
    public void testAdd16BitBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .build();

        // Put values to add in the registers
        short valueOne = 0b0101100000000000;
        short valueTwo = 0b0000111111111111;
        cpuRegister.set16BitRegister(CPURegister.Register.HL, valueOne);
        cpuRegister.set16BitRegister(CPURegister.Register.DE, valueTwo);

        //
        int cycles = cpu.add(CPURegister.Register.HL,
                CPURegister.Register.DE,
                false);

        // Check sum
        assertEquals((short) (valueOne + valueTwo),
                cpuRegister.get16BitRegisterValue(CPURegister.Register.HL));

        // Check flags
        assertEquals((short) (valueOne + valueTwo) == 0, flagRegister.z);
        assertEquals(false, flagRegister.n);
        assertEquals(true, flagRegister.h);
        assertEquals(false, flagRegister.c);

        // Check cycles
        assertEquals(8, cycles);

    }

    @Test
    public void testSwapBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().flagRegister(flagRegister).cpuRegister(cpuRegister).build();

        // Set an arbitrary value as the A register and swap its nibbles
        cpuRegister.A = arbitraryByte;
        int cycles = cpu.swap(CPURegister.Register.A);

        // Check that the halves of the byte (nibbles, if you will) have been
        // swapped correctly
        assertEquals(0b00010100, cpuRegister.A);

        // Check flags
        assertFalse(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(8, cycles);
    }

    @Test
    public void testSwapHL() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder()
                .flagRegister(flagRegister)
                .memory(memory)
                .cpuRegister(cpuRegister)
                .build();


        // Set an arbitrary byte in memory, point the HL register at that
        byte easyByte = 0b00001111;
        cpuRegister.setRegister(CPURegister.Register.HL, splitAddress[0], splitAddress[1]);
        memory.writeByte(arbitraryMemoryAddress, easyByte);
        int cycles = cpu.swap(CPURegister.Register.HL_ADDRESS);

        // Check that the halves of the byte (nibbles, if you will) have been
        // swapped correctly
        assertEquals((byte) 0b11110000, memory.readByte(arbitraryMemoryAddress));

        // Check flags
        assertFalse(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(16, cycles);
    }

    @Test
    public void testDaaBasic() {
        fail();
    }

    @Test
    public void testComplementBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().flagRegister(flagRegister).cpuRegister(cpuRegister).build();

        // Set the A register to contain an arbitrary byte, then complement that value
        cpuRegister.A = arbitraryByte;
        int cycles = cpu.complement();

        // Check that the A register is the complement of arbitraryByte
        assertEquals((byte) 0b10111110, cpuRegister.A);

        // Check flags
        assertTrue(flagRegister.n);
        assertTrue(flagRegister.h);

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testComplementCarryFlagBasic() {
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().flagRegister(flagRegister).build();

        // Set carry flag to true, then complement that (bit it to false)
        flagRegister.c = true;
        int cycles = cpu.complementCarryFlag();

        // These should be false no matter what
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);

        // This should be complemented
        assertFalse(flagRegister.c);

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testSetCarryFlag() {
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().flagRegister(flagRegister).build();

        // Set carry flag to false, then call setCarryFlag to turn it to be true
        flagRegister.c = false;
        int cycles = cpu.setCarryFlag();

        // These should be false no matter what
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);

        // This should be complemented
        assertTrue(flagRegister.c);

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testNoOperation() {
        FlagRegister flagRegister = new FlagRegister();
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder()
                .flagRegister(flagRegister)
                .programCounter(pc)
                .cpuRegister(cpuRegister)
                .build();

        // Run a "no operation," which should change no flags, change no
        // registers, change no memory, and consume no bytes
        int cycles = cpu.noOperation();

        // Assume all flags defaulted to false
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertFalse(flagRegister.c);
        assertFalse(flagRegister.z);

        // Assume all registers defaulted to zero
        assertEquals(0, cpuRegister.A);
        assertEquals(0, cpuRegister.B);
        assertEquals(0, cpuRegister.C);
        assertEquals(0, cpuRegister.D);
        assertEquals(0, cpuRegister.E);
        assertEquals(0, cpuRegister.F);
        assertEquals(0, cpuRegister.H);
        assertEquals(0, cpuRegister.L);
        assertEquals(0, cpuRegister.SP);
        assertEquals(0, cpuRegister.PC);

        // Program counter should stay at 0
        assertEquals(0, pc.getAddr());

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testHaltBasic() {
        CPU cpu = new CPU.Builder().build();

        // Check that CPU begins in normal state
        assertEquals(CPU.CPUState.NORMAL, cpu.getCpuState());

        int cycles = cpu.halt();

        // Check cycles
        assertEquals(4, cycles);

        // Check that CPU status has changed
        assertEquals(CPU.CPUState.HALTED, cpu.getCpuState());
    }

    @Test
    public void testStopBasic() {
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().memory(memory).programCounter(pc).build();

        // Check that CPU begins in normal state
        assertEquals(CPU.CPUState.NORMAL, cpu.getCpuState());

        // Load 0x00 in as the next instruction b/c gameboy spec requires 0x00
        // to come after STOP
        memory.loadBytesToRom(new byte[]{0x00}, 0);

        int cycles = cpu.stop();

        // Check cycles
        assertEquals(4, cycles);

        // Check that CPU status has changed
        assertEquals(CPU.CPUState.STOPPED, cpu.getCpuState());

        // Check that 1 byte was consumed
        assertEquals(1, pc.getAddr());
    }

    @Test
    public void testDisableInterrupts() {
        CPU cpu = new CPU.Builder().build();

        // First, make sure the CPU allows interrupts
        assert cpu.getInterruptState().equals(INTERRUPTABLE);

        // Now, enable interrupts
        int cycles = cpu.disableInterrupts();

        assertEquals(4, cycles);
        assertEquals(UNINTERRUPTABLE, cpu.getInterruptState());
    }

    @Test
    public void testEnableInterrupts() {
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().memory(memory).build();

        // First, make sure the CPU is does not allow interrupts
        cpu.disableInterrupts();

        // Now, enable interrupts
        int cycles = cpu.enableInterrupts();

        assertEquals(4, cycles);

        // Check that it goes to INTERRUPTABLE_NEXT_COMMAND
        assertEquals(INTERRUPTABLE_NEXT_COMMAND, cpu.getInterruptState());

        // Run ANY command. In this case- put a NO OP as the next command. Run
        // via cpu.executeInstruction because this is the method that sets the
        // interrupt state to INTERRUPTABLE. Just running cpu.noNoperation()
        // does not change interrupt status.
        memory.loadBytesToRom(new byte[]{0x00}, 0);
        cpu.executeInstruction();

        // Check that CPU enters INTERRUPTABLE
        assertEquals(INTERRUPTABLE, cpu.getInterruptState());
    }

    @Test
    public void testRotateLeftBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().cpuRegister(cpuRegister).flagRegister(flagRegister).build();

        // Put an arbitrary value in the A register
        cpuRegister.A = arbitraryByte;

        // Set the carry flag to be true
        flagRegister.c = true;

        // Rotate the bits in the A register 1 to the left, replacing the
        // rightmost checkBit with the old carry value
        int cycles = cpu.rotateLeft(CPURegister.Register.A);

        // All bits moved 1 to the left, with the rightmost checkBit replaced by 1
        assertEquals((byte) 0b10000011, cpuRegister.A);

        // Check flags
        assertFalse(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertEquals(false, flagRegister.c);

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testRotateLeftLoseByte() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().cpuRegister(cpuRegister).flagRegister(flagRegister).build();

        // Put a value in the A register with leftmost checkBit equal to 1
        cpuRegister.A = (byte) 0b10000000;

        // Rotate the bits in the A register to the left
        int cycles = cpu.rotateLeft(CPURegister.Register.A);

        // The left checkBit should be shifted away
        assertEquals(0, cpuRegister.A);

        // Check flags
        assertTrue(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertTrue(flagRegister.c);

        // Check cycles
        assertEquals(4, cycles);

    }

    @Test
    public void testRotateLeftThroughCarry() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().cpuRegister(cpuRegister).flagRegister(flagRegister).build();

        // Put a value in the A register with leftmost checkBit equal to 1
        cpuRegister.A = (byte) 0b10000000;

        // Rotate the byte
        int cycles = cpu.rotateLeftThroughCarry(CPURegister.Register.A);

        // Check that the leftmost checkBit carried over to checkBit 0
        assertTrue(Util.checkBit(cpuRegister.A, (byte) 0));

        // Check flags
        assertFalse(flagRegister.z);
        assertFalse(flagRegister.n);
        assertFalse(flagRegister.h);
        assertTrue(flagRegister.c);

        // Check cycles
        assertEquals(4, cycles);
    }

    @Test
    public void testRotateRightBasic() {
        fail();
    }

    @Test
    public void testRotateRightThroughCarryBasic() {
        fail();
    }

    @Test
    public void testCheckBitBasic() {
        CPURegister cpuRegister = new CPURegister();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder().cpuRegister(cpuRegister).flagRegister(flagRegister).build();

        // Set a byte with all false bits
        byte zeroByte = 0b00000000;
        cpuRegister.A = zeroByte;

        // Set the z flag to true- we want to be sure cpu.checkBit changes it
        flagRegister.z = true;

        // Check that checkBit 0 is false
        int cycles = cpu.checkBit(CPURegister.Register.A, (byte) 0);

        // Check that the z flag has been bit to false
        assertFalse(flagRegister.z);

        // Check other flags
        assertFalse(flagRegister.n);
        assertTrue(flagRegister.h);

        // Check cycles
        assertEquals(cycles, 4);
    }

    @Test
    public void testSetBitBasic() {
        CPURegister cpuRegister = new CPURegister();
        CPU cpu = new CPU.Builder().cpuRegister(cpuRegister).build();

        // Set an arbitrary byte in Register A
        cpuRegister.A = 0b00000000;

        // Set bit 0 to true
        int cycles = cpu.setBit(CPURegister.Register.A, (byte) 0, true);

        // Check that bit 0 has changed
        assertEquals(0b00000001, cpuRegister.A);
        assertEquals(8, cycles);
    }

    @Test
    public void testSetBitFalse() {
        CPURegister cpuRegister = new CPURegister();
        CPU cpu = new CPU.Builder().cpuRegister(cpuRegister).build();

        // Set an arbitrary byte in Register A
        cpuRegister.A = 0b01111111;

        // Set bit 0 to false
        int cycles = cpu.setBit(CPURegister.Register.A, (byte) 0, false);

        // Check that bit 0 has changed
        assertEquals(0b01111110, cpuRegister.A);
        assertEquals(8, cycles);
    }

    @Test
    public void testSetBitHL() {
        CPURegister cpuRegister = new CPURegister();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder().memory(memory).cpuRegister(cpuRegister).build();

        // Put HL to point to an arbitrary address
        cpuRegister.set16BitRegister(CPURegister.Register.HL, (short) arbitraryMemoryAddress);

        // Set bit 0 to true (it will be false by default)
        int cycles = cpu.setBit(CPURegister.Register.HL_ADDRESS, (byte) 0, true);

        // Check that bit 0 has changed
        assertEquals(0b00000001,
                Util.unsignedShortToInt(memory.readByte(arbitraryMemoryAddress)));
        assertEquals(16, cycles);
    }

    @Test
    public void testJumpBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder()
                .memory(memory)
                .cpuRegister(cpuRegister)
                .programCounter(pc)
                .build();

        // Set the next two bytes in ROM to be a memory address
        memory.loadBytesToRom(new byte[]{splitAddress[1], splitAddress[0]}, 0);
        int cycles = cpu.jump();

        // Ensure that the program counter points to "arbitraryMemoryAddress" now
        assertEquals(arbitraryMemoryAddress, pc.getAddr());
        assertEquals(12, cycles);
    }

    @Test
    public void testJumpZFlagBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .memory(memory)
                .flagRegister(flagRegister)
                .cpuRegister(cpuRegister)
                .programCounter(pc)
                .build();

        // Set the zFlag to true- we should jump
        flagRegister.z = true;

        // Set the next two bytes in ROM to be a memory address
        memory.loadBytesToRom(new byte[]{splitAddress[1], splitAddress[0]}, 0);
        int cycles = cpu.jumpZFlag(true);

        // Ensure that the program counter points to "arbitraryMemoryAddress" now
        assertEquals(arbitraryMemoryAddress, pc.getAddr());
        assertEquals(12, cycles);
    }

    @Test
    public void testJumpZFlagFalse() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .memory(memory)
                .flagRegister(flagRegister)
                .cpuRegister(cpuRegister)
                .programCounter(pc)
                .build();

        // Set the zFlag to true- we should jump
        flagRegister.z = false;

        // Set the next two bytes in ROM to be a memory address
        memory.loadBytesToRom(new byte[]{splitAddress[1], splitAddress[0]}, 0);
        int cycles = cpu.jumpZFlag(true);

        // Ensure that the program counter points to 2- because it starts at 0
        // and read two bytes from ROM for the jumpZFlag command
        assertEquals(2, pc.getAddr());
        assertEquals(12, cycles);
    }

    @Test
    public void testJumpCFlagBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .memory(memory)
                .flagRegister(flagRegister)
                .cpuRegister(cpuRegister)
                .programCounter(pc)
                .build();

        // Set the zFlag to true- we should jump
        flagRegister.c = true;

        // Set the next two bytes in ROM to be a memory address
        memory.loadBytesToRom(new byte[]{splitAddress[1], splitAddress[0]}, 0);
        int cycles = cpu.jumpCFlag(true);

        // Ensure that the program counter points to "arbitraryMemoryAddress" now
        assertEquals(arbitraryMemoryAddress, pc.getAddr());
        assertEquals(12, cycles);
    }

    @Test
    public void testJumpCFlagFalse() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .memory(memory)
                .flagRegister(flagRegister)
                .cpuRegister(cpuRegister)
                .programCounter(pc)
                .build();

        // Set the zFlag to true- we should jump
        flagRegister.c = false;

        // Set the next two bytes in ROM to be a memory address
        memory.loadBytesToRom(new byte[]{splitAddress[1], splitAddress[0]}, 0);
        int cycles = cpu.jumpCFlag(true);

        // Ensure that the program counter points to 2- because it starts at 0
        // and read two bytes from ROM for the jumpZFlag command
        assertEquals(2, pc.getAddr());
        assertEquals(12, cycles);
    }

    @Test
    public void testJump16Bit() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .programCounter(pc)
                .build();

        // Set the DE register to a 16-bit address
        cpuRegister.setRegister(CPURegister.Register.HL,
                splitAddress[0],
                splitAddress[1]);

        // Jump to that address
        int cycles = cpu.jump(CPURegister.Register.HL);

        // Check that the program counter holds that address now
        assertEquals(arbitraryMemoryAddress, pc.getAddr());
        assertEquals(4, cycles);
    }

    @Test
    public void testJumpRelativeBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .programCounter(pc)
                .build();

        // Set the next byte in ROM to be part of a memory address
        memory.loadBytesToRom(new byte[]{arbitraryByte}, 0);

        // Set to current address + next byte in RAM: current address will be
        // 1 + next byte
        int cycles = cpu.jumpRelative();

        // Check that the program counter holds that address now
        assertEquals(arbitraryByte + 1, pc.getAddr());
        assertEquals(8, cycles);
    }

    @Test
    public void testJumpZFlagRelativeBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .memory(memory)
                .programCounter(pc)
                .build();

        // Set the next byte in ROM to be part of a memory address
        memory.loadBytesToRom(new byte[]{arbitraryByte}, 0);

        // Set z flag true
        flagRegister.z = true;

        // Relative jump if Z flag is true
        int cycles = cpu.jumpZFlagRelative(true);

        // Ensure that we jumped
        assertEquals(arbitraryByte + 1, pc.getAddr());
        assertEquals(8, cycles);
    }

    @Test
    public void testJumpZFlagRelativeFalse() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .memory(memory)
                .programCounter(pc)
                .build();

        // Set the next byte in ROM to be part of a memory address
        memory.loadBytesToRom(new byte[]{arbitraryByte}, 0);

        // Set z flag true
        flagRegister.z = false;

        // Relative jump if Z flag is true
        int cycles = cpu.jumpZFlagRelative(true);

        // Ensure that we jumped
        assertEquals(1, pc.getAddr());
        assertEquals(8, cycles);
    }

    @Test
    public void testJumpCFlagRelativeBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .memory(memory)
                .programCounter(pc)
                .build();

        // Set the next byte in ROM to be part of a memory address
        memory.loadBytesToRom(new byte[]{arbitraryByte}, 0);

        // Set z flag true
        flagRegister.c = true;

        // Relative jump if Z flag is true
        int cycles = cpu.jumpCFlagRelative(true);

        // Ensure that we jumped
        assertEquals(arbitraryByte + 1, pc.getAddr());
        assertEquals(8, cycles);
    }

    @Test
    public void testJumpCFlagRelativeFalse() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .flagRegister(flagRegister)
                .memory(memory)
                .programCounter(pc)
                .build();

        // Set the next byte in ROM to be part of a memory address
        memory.loadBytesToRom(new byte[]{arbitraryByte}, 0);

        // Set z flag true
        flagRegister.c = false;

        // Relative jump if Z flag is true
        int cycles = cpu.jumpCFlagRelative(true);

        // Ensure that we jumped
        assertEquals(1, pc.getAddr());
        assertEquals(8, cycles);
    }

    @Test
    public void testCallBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .programCounter(pc)
                .build();

        // Make sure SP isn't 0 so we don't hit an over/underflow when call
        // increments/decrements the stack pointer
        cpuRegister.SP = 100;

        // Set program counter to some arbitrary address
        byte programCounterAddress = 0b00001111;
        pc.setAddr(programCounterAddress);

        // Set the next two bytes in memory to be some different arbitrary address
        // Note: load them into ROM in reverse order because gameboy is little-
        // endian
        memory.loadBytesToRom(new byte[]{splitAddress[1], splitAddress[0]}, 0);

        // Run cpu.call(). This should push the PC addr onto the stack and
        // then move pc.getAddr to arbitraryMemoryAddress
        cpu.call();

        // Check that the top two bytes on the stack are the address that was
        // in the program counter
        cpu.pop(CPURegister.Register.DE);

        int actualPCAddress = Util.unsignedShortToInt(
                Util.concatBytes(cpuRegister.D, cpuRegister.E));
        assertEquals(programCounterAddress, actualPCAddress);

        // Check that the current PC address is the same as the bytes loaded
        // in from memory
        assertEquals(arbitraryMemoryAddress, pc.getAddr());
    }

    // Test call() implementation starting at an address greater than
    // Short.MAX_VALUE
    @Test
    public void testCallStartingAtLargePCAddress() {
        fail();
    }

    @Test
    public void testCallCFlag() {
        fail();
    }

    @Test
    public void testCallCFlagBasic() {
        fail();
    }

    @Test
    public void testRestBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .programCounter(pc)
                .build();

        // Set program counter address to some address
        pc.setAddr(arbitraryMemoryAddress);

        // Set SP to any value where it won't over/underflow
        cpuRegister.SP = 0x0F;

        // Rest should push the current program counter address to the stack
        // and then set program counter to address 8
        int cycles = cpu.rest((byte) 8);

        // Check that the pc is set to the correct address
        assertEquals(8, pc.getAddr());

        // Check that current address was added to stack properly by popping it
        // off of the stack and into DE, then comparing the DE register to the
        // true value
        cpu.pop(CPURegister.Register.DE);
        assertEquals(splitAddress[0], cpuRegister.D);
        assertEquals(splitAddress[1], cpuRegister.E);

        // Check cycles
        assertEquals(32, cycles);
    }

    @Test
    public void testRetBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .programCounter(pc)
                .build();

        // Set the stack pointer so we don't overflow/underflow
        cpuRegister.SP = 0x0F;

        // Put two bytes onto the stack
        cpuRegister.setRegister(CPURegister.Register.DE,
                splitAddress[0],
                splitAddress[1]);
        cpu.push(CPURegister.Register.DE);

        int cycles = cpu.ret();

        // Test that ret successfully removed those bytes from stack
        int stackAddr = Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(
                        CPURegister.Register.SP));
        assertEquals(0, memory.readByte(stackAddr));

        // Test that program counter becomes the addr from the stack
        assertEquals(arbitraryMemoryAddress, pc.getAddr());

        // Check cycles
        assertEquals(8, cycles);
    }

    @Test
    public void testRetZBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .flagRegister(flagRegister)
                .programCounter(pc)
                .build();

        // Set the stack pointer so we don't overflow/underflow
        cpuRegister.SP = 0x0F;

        // Put two bytes onto the stack
        cpuRegister.setRegister(CPURegister.Register.DE,
                splitAddress[0],
                splitAddress[1]);
        cpu.push(CPURegister.Register.DE);

        // Set the Z flag to be true
        flagRegister.z = true;

        int cycles = cpu.retZ(true);

        // Test that ret successfully removed those bytes from stack
        int stackAddr = Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(
                        CPURegister.Register.SP));
        assertEquals(0, memory.readByte(stackAddr));

        // Test that program counter becomes the addr from the stack
        assertEquals(arbitraryMemoryAddress, pc.getAddr());

        // Check cycles
        assertEquals(8, cycles);
    }

    @Test
    public void testRetZFalse() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .flagRegister(flagRegister)
                .programCounter(pc)
                .build();

        // Set the stack pointer so we don't overflow/underflow
        cpuRegister.SP = 0x0F;

        // Put two bytes onto the stack
        cpuRegister.setRegister(CPURegister.Register.DE,
                splitAddress[0],
                splitAddress[1]);
        cpu.push(CPURegister.Register.DE);

        // Set the Z flag to be true
        flagRegister.z = true;

        int cycles = cpu.retZ(false);

        // Test that ret failed to those bytes from stack
        int stackAddr = Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(
                        CPURegister.Register.SP));
        assertEquals(splitAddress[1], memory.readByte(stackAddr));

        // Test that program counter does not change
        assertEquals(0, pc.getAddr());

        // Check cycles
        assertEquals(8, cycles);
    }

    @Test
    public void testRetCBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .flagRegister(flagRegister)
                .programCounter(pc)
                .build();

        // Set the stack pointer so we don't overflow/underflow
        cpuRegister.SP = 0x0F;

        // Put two bytes onto the stack
        cpuRegister.setRegister(CPURegister.Register.DE,
                splitAddress[0],
                splitAddress[1]);
        cpu.push(CPURegister.Register.DE);

        // Set the Z flag to be true
        flagRegister.c = true;

        int cycles = cpu.retC(true);

        // Test that ret successfully removed those bytes from stack
        int stackAddr = Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(
                        CPURegister.Register.SP));
        assertEquals(0, memory.readByte(stackAddr));

        // Test that program counter becomes the addr from the stack
        assertEquals(arbitraryMemoryAddress, pc.getAddr());

        // Check cycles
        assertEquals(8, cycles);
    }

    @Test
    public void testRetCFalse() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        FlagRegister flagRegister = new FlagRegister();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .flagRegister(flagRegister)
                .programCounter(pc)
                .build();

        // Set the stack pointer so we don't overflow/underflow
        cpuRegister.SP = 0x0F;

        // Put two bytes onto the stack
        cpuRegister.setRegister(CPURegister.Register.DE,
                splitAddress[0],
                splitAddress[1]);
        cpu.push(CPURegister.Register.DE);

        // Set the Z flag to be true
        flagRegister.c = true;

        int cycles = cpu.retC(false);

        // Test that ret failed to those bytes from stack
        int stackAddr = Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(
                        CPURegister.Register.SP));
        assertEquals(splitAddress[1], memory.readByte(stackAddr));

        // Test that program counter does not change
        assertEquals(0, pc.getAddr());

        // Check cycles
        assertEquals(8, cycles);
    }

    @Test
    public void testRetiBasic() {
        CPURegister cpuRegister = new CPURegister();
        ProgramCounter pc = new ProgramCounter();
        Memory memory = new Memory();
        CPU cpu = new CPU.Builder()
                .cpuRegister(cpuRegister)
                .memory(memory)
                .programCounter(pc)
                .build();

        // Set the stack pointer so we don't overflow/underflow
        cpuRegister.SP = 0x0F;

        // Put two bytes onto the stack
        cpuRegister.setRegister(CPURegister.Register.DE,
                splitAddress[0],
                splitAddress[1]);
        cpu.push(CPURegister.Register.DE);

        int cycles = cpu.ret();

        // Test that ret successfully removed those bytes from stack
        int stackAddr = Util.unsignedShortToInt(
                cpuRegister.get16BitRegisterValue(
                        CPURegister.Register.SP));
        assertEquals(0, memory.readByte(stackAddr));

        // Test that program counter becomes the addr from the stack
        assertEquals(arbitraryMemoryAddress, pc.getAddr());

        // Check cycles
        assertEquals(8, cycles);

        // Check that interrupts are enabled
        assertEquals(INTERRUPTABLE, cpu.getInterruptState());
    }

}
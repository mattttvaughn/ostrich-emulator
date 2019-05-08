package mattpvaughn.io.github.emulator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// Memory for the ostrich GameBoy emulator
// By Matt Vaughn: http://mattpvaughn.github.io/

public class Memory {

    // Default memory size is from 0-0xFFFF (0xFFFF + 1 bytes)
    private byte[] memory = new byte[0xFFFF + 1];

    // The largest memory address which we are allowed to retrieve memory from.
    // When memory bank controllers are added this value should end up being
    // computed in the constructor.
    public static final int MAX_MEMORY_SIZE = 0xFFFF + 1;

    // The largest ROM address which we can retrieve memory from
    public static final int MAX_ROM_SIZE = 0xFFFF + 1;

    // Default boot rom location
    // TODO: load this in as a relative file
    private final String bootRomLoc = "/home/matt/Development/ostrich-emulator/src/main/java/mattpvaughn/io/github/emulator/rom/boot.rom";

    // The size of the boot rom, indicates where the game rom will be loaded
    private final int bootRomSize = 0xFF;

    // Boot and game ROMs
    private byte[] rom = new byte[0xFFFF];

    public Memory() {
        this.loadBootRom();
    }

    // Returns a byte of ROM at a specified address
    // TODO- User ROM should be stored in address 0x0150-0x8000
    public byte readRom(int address) {
        if (address > rom.length || address < 0) {
            throw new IllegalArgumentException("Address requested is out of ROM bounds! " + address + " out of " + MAX_MEMORY_SIZE);
        }
        return rom[address];
    }

    // Write a byte of ROM to a specific address
    public void writeByte(int address, byte value) {
        if (address > MAX_MEMORY_SIZE || address < 0) {
            throw new IllegalArgumentException("Address requested is out of bounds! " + address + " out of " + MAX_MEMORY_SIZE);
        }
        this.memory[address] = value;
    }

    public byte readByte(int address) {
        if (address > MAX_MEMORY_SIZE || address < 0) {
            throw new IllegalArgumentException("Address requested is out of bounds! " + address + " out of " + MAX_MEMORY_SIZE);
        }
        return this.memory[address];
    }

    // Loads the boot rom into memory
    // The boot rom path should be located at the relative path rom/boot.rom
    private void loadBootRom() {
        File file = new File(bootRomLoc);
        // Load into the rom byte[] in memory
        loadByteArray(rom, readFileToByteArray(file), 0);
    }

    // Load an array of bytes into a large byte array
    // Parameters:
    //      writeTo: the array to which the bytes will be written
    //      bytes: the bytes to write into writeTo
    //      addr: the address in writeTo the first byte should be written
    private void loadByteArray(byte[] writeTo, byte[] bytes, int addr) {
        if (addr < 0) {
            throw new IllegalArgumentException("Attempted to write to invalid memory location: " + addr);
        }
        if (bytes.length + addr > writeTo.length) {
            throw new IllegalArgumentException("Attempted to write past the end of main memory!");
        }
        for (int i = 0; i < bytes.length; i++) {
            writeTo[addr + i] = bytes[i];
        }
    }

    // Loads an array of bytes into ROM starting at location (addr)
    //      bytes: the bytes to write into writeTo
    //      addr: the address in writeTo the first byte should be written
    public void loadBytesToRom(byte[] bytes, int addr) {
        loadByteArray(rom, bytes, addr);
    }

    // Load 160 bytes from an address in RAM into OAM (0xFE00 to 0xFE9F). Used
    // for DMA transfer
    public void loadToOAM(int address) {
        //
        loadByteArray(memory, readBytes(memory, address, 160), 0xFE00);
    }

    // Load a subset of bytes from a byte[], given a start address and the
    // number of bytes you want
    private byte[] readBytes(byte[] source, int start, int length) {
        if (start < 0 || start + length >= source.length) {
            throw new IndexOutOfBoundsException();
        }
        // Iterate through source array, copying bytes from [start] to [start +
        // length]
        byte[] retBytes = new byte[length];
        for (int i = start; i < start + length; i++) {
            retBytes[i - start] = source[i];
        }

        return retBytes;
    }

    // Reads the bytes of a file into a byte array
    // TODO: this may create problems if someone tries to load an unnecessarily
    // large ROM and we run out of memory
    private static byte[] readFileToByteArray(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException io) {
            Utils.log("Unable to read bytes from file: " + file + ".\r\n" + io);
        }
        return null;
    }

    // Attach a game ROM file to the memory. This will be important for normal
    // game playing, but don't include it in the constructor because the emulator
    // can run solely on the boot ROM itself (nice to not need to load a whole
    // game for testing)
    public void attachGameFile(File gameRom) {
        // This should be pretty efficient because the ROMs are small, but it
        // might be worth looking into reading ROMs as buffers
        this.loadByteArray(rom, readFileToByteArray(gameRom), bootRomSize);
    }
}

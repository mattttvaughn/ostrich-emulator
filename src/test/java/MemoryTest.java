import mattpvaughn.io.github.emulator.Memory;
import mattpvaughn.io.github.emulator.cpu.Util;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MemoryTest {

    // Test loading the boot rom into ROM
    @Test
    public void testLoadBootRom() {
        // Create a new memory object- this MUST load in loadBootRom by default
        Memory memory = new Memory();

        // Check that bytes loaded into ROM in memory match expected values
        assertEquals(memory.readRom(0), (byte) 0x31);
        assertEquals(memory.readRom(1), (byte) 0xFE);
        assertEquals(memory.readRom(2), (byte) 0xFF);
    }

    // Example GameBoy game ROM
    public static final String exampleGameRom = "/home/matt/Development/ostrich-emulator/src/main/java/mattpvaughn/io/github/emulator/rom/tetris.gb";

    // Test loading in an example game to ROM
    @Test
    public void testLoadGameRom() {
        Memory memory = new Memory();

        // Load the file into ROM
        File exampleGameRomFile = new File(exampleGameRom);
        memory.attachGameFile(exampleGameRomFile);

        // Check that bytes are loaded into expected location
        assertEquals(memory.readRom(0xFF), (byte) 0xC3);
        assertEquals(memory.readRom(0x100), (byte) 0x0C);
        assertEquals(memory.readRom(0x101), (byte) 0x02);
    }

    @Test
    public void testReadWriteByteBasic() {
        Memory memory = new Memory();

        short memoryAddress = (short) 0xCDDD;
        byte arbitraryByte = 55;

        memory.writeByte(Util.unsignedShortToInt(memoryAddress), arbitraryByte);

        assertEquals(arbitraryByte,
                memory.readByte(Util.unsignedShortToInt(memoryAddress)));
    }

    @Test
    public void testReadByteOutOfBounds() {
        Memory memory = new Memory();
        try {
            memory.readByte(-1);
            fail();
        } catch (IllegalArgumentException e) {

        }

        try {
            memory.readByte(Integer.MAX_VALUE);
            fail();
        } catch (IllegalArgumentException e) {

        }
    }

}

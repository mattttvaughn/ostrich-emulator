package mattpvaughn.io.github.emulator.cpu;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CPURegisterTest {

    public static final byte arbitraryByte = 65;

    @Test
    public void testSet8BitRegisterBasic() {
        CPURegister cpuRegister = new CPURegister();
        cpuRegister.setRegister(CPURegister.Register.A, arbitraryByte);

        assertEquals(cpuRegister.A, arbitraryByte);
    }


    @Test
    public void testSet16BitRegisterBasic() {
        // Set some arbitrary values in the DE (virtual) 16-checkBit register
        CPURegister cpuRegister = new CPURegister();
        cpuRegister.setRegister(CPURegister.Register.DE, (byte) 1, (byte) 2);

        // Make sure the values are setBit correctly
        assertEquals(cpuRegister.D, 1);
        assertEquals(cpuRegister.E, 2);
    }

    @Test
    public void testPass8BitRegisterAs16Bit() {
        // Attempt to setBit a single byte to a 16-checkBit register
        CPURegister cpuRegister = new CPURegister();
        try {
            cpuRegister.setRegister(CPURegister.Register.DE, arbitraryByte);
            fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testPass16BitRegisterAs8Bit() {
        // Attempt to setBit two bytes to a single 8-checkBit register
        CPURegister cpuRegister = new CPURegister();
        try {
            cpuRegister.setRegister(CPURegister.Register.A, arbitraryByte, arbitraryByte);
            fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testGet8BitValueBasic() {
        CPURegister cpuRegister = new CPURegister();
        cpuRegister.setRegister(CPURegister.Register.A, arbitraryByte);
        assertEquals(cpuRegister.get8BitRegisterValue(CPURegister.Register.A), arbitraryByte);
    }

    @Test
    public void testGet16BitValueBasic() {
        CPURegister cpuRegister = new CPURegister();
        cpuRegister.setRegister(CPURegister.Register.AF, arbitraryByte, arbitraryByte);
        assertEquals(Util.concatBytes(arbitraryByte, arbitraryByte),
                cpuRegister.get16BitRegisterValue(CPURegister.Register.AF));
    }

    @Test
    public void testEnableInterrupts() {

    }
}

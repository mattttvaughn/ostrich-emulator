package mattpvaughn.io.github.emulator.cpu;

import org.junit.Test;

import static org.junit.Assert.*;

public class CPUUtilsTest {

    // 16-checkBit number written in binary form
    private static final short nShort = 0b0101010110101010;

    // 8-checkBit number matching the least significant half of the 16-checkBit number
    // Represent 170 (0xAA):
    private static final byte nLeastSignificantHalf = (byte) 0b10101010;

    // 8-checkBit number matching the most significant (first) half of the 16-checkBit
    // number. Represent 85 (0x55)
    private static final byte nMostSignificantHalf = 0b01010101;

    @Test
    public void testConcatBytesBasic() {
        // Combine two bytes into
        assertEquals(nShort, Util.concatBytes(nMostSignificantHalf,
                nLeastSignificantHalf));
    }

    // An arbitrary unsigned number between 0-255. We choose a number greater
    // than 128 here because then our method has to deal with java's two's
    // complement representation of the number
    private static final int intBetween128And255 = 234;

    // Convert an arbitrary unsigned byte (as stored in a byte variable) to an
    // equivalently valued int
    @Test
    public void testUnsigedByteToIntTwosComplement() {
        assertEquals(intBetween128And255,
                Util.unsignedByteToInt((byte) intBetween128And255));
    }

    private static final int arbitraryPositiveIntUnder128 = 99;

    // Test converting a valid positive byte value (1-127) from a byte (as
    // shortened by java's (byte) cast) into an int value
    @Test
    public void testUnsignedByteToIntBasic() {
        assertEquals(arbitraryPositiveIntUnder128,
                Util.unsignedByteToInt((byte) arbitraryPositiveIntUnder128));
    }

    // Test splitting an arbitrary short into two bytes
    @Test
    public void testSplitIntToBytesBasic() {
        byte[] splitted = Util.splitShortToBytes(nShort);
        assertEquals(nMostSignificantHalf, splitted[0]);
        assertEquals(nLeastSignificantHalf, splitted[1]);
    }

    @Test
    public void testCheckBit() {
        byte checkMe = 0b01010101;
        assertTrue(Util.checkBit(checkMe, (byte) 0));
        assertFalse(Util.checkBit(checkMe, (byte) 1));
        assertTrue(Util.checkBit(checkMe, (byte) 2));
        assertFalse(Util.checkBit(checkMe, (byte) 3));
        assertTrue(Util.checkBit(checkMe, (byte) 4));
        assertFalse(Util.checkBit(checkMe, (byte) 5));
        assertTrue(Util.checkBit(checkMe, (byte) 6));
        assertFalse(Util.checkBit(checkMe, (byte) 7));
    }

}

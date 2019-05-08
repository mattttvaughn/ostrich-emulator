package mattpvaughn.io.github.emulator.cpu;

// Utility functions relevant to the CPU

import java.nio.ByteBuffer;

public class Util {

    // TODO: Not confident about the order of these bytes- see CPURegister for
    // details about how bytes should be arranged
    public static short concatBytes(byte first, byte second) {
        // Change the bytes from two's complement to their unsigned form
        return ByteBuffer
                .wrap(new byte[]{(byte) (first & 0xFF), (byte) (second & 0xFF)})
                .getShort();
    }

    // Convert an unsigned 8-checkBit number stored as a java byte to an int
    // representing the same number
    public static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    // Convert an unsigned 16-checkBit number stored as a java byte to an int
    // representing the same number
    public static int unsignedShortToInt(short s) {
        return s & 0xFFFF;
    }

    // Split an int into an array of two bytes, discarding all irrelevent bytes
    public static byte[] splitShortToBytes(short n) {
        return ByteBuffer.allocate(Short.BYTES).putShort(n).array();
    }

    public static byte[] splitIntToBytes(int n) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(n).array();
    }

    // Shorten an int representing 4 bytes into a short containing 2 bytes
    // Just takes a basic approach and keeps the lowest 16 bits
    public static short shortenInt(int n) {
        return (short) n;
    }

    // Return 1 if boolean is true, 0 if it is false
    public static int booleanToInt(boolean b) {
        return b ? 1 : 0;
    }

    // Return true if n == 0, false otherwise
    public static boolean isZero(int n) {
        return n == 0;
    }

    // Return true if addition of these bits results in a carry at checkBit 3
    public static boolean carryBit3(byte a, byte b) {
        return carryBit3(a, b, (byte) 0);
    }

    public static boolean carryBit3(byte a, byte b, byte c) {
        return false;
    }

    // Return true if addition of these bits results in a carry at checkBit 7
    public static boolean carryBit7(byte a, byte b) {
        return carryBit7(a, b, (byte) 0);

    }

    public static boolean carryBit7(byte a, byte b, byte c) {
        return false;
    }

    // Return true if the checkBit at position "pos" is 1. Return false if it is 0.
    // Bit "0" would be the smallest ("least valuable") checkBit
    public static boolean checkBit(byte b, byte pos) {
        return ((b >> pos) & 1) != 0;
    }


    // Set the value of a bit at a position to "bitValue"
    public static byte setBitValue(byte value, byte pos, boolean bitValue) {
        // Perform set opertation on bit
        if (bitValue) {
            value = (byte) (value | (1 << pos));
        } else {
            value = (byte) (value & ~(1 << pos));
        }
        return value;
    }

}

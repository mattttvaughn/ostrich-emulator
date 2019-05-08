package mattpvaughn.io.github.emulator.cpu;

// CPU registers for the ostrich GameBoy emulator. Provides the CPU an API to
// setBit 8-bit or 16-bit registers given one or two 8-bit values.
//
// By Matt Vaughn: http://mattpvaughn.github.io/

public class CPURegister {

    // Note: sometimes the 8-bit registers are combined to make 16-bit
    // registers, like AF, BC, DE, HL. Initial values set by boot.rom
    // As a 16-bit register, A would be 15..8, and F would be 7..0
    public byte A = 0;
    public byte B = 0;
    public byte C = 0;
    public byte D = 0;
    public byte E = 0;
    public byte F = 0;
    public byte H = 0;
    public byte L = 0;

    // This registers will always be read to/written as 16-bits
    public short SP = 0;
    public short PC = 0;

    public enum Register {
        // All possible registers: includes HL_ADDRESS in place of (HL)
        A, B, C, D, E, F, H, L, BC, DE, HL, SP, PC, AF, HL_ADDRESS;

        // Returns true if a register is a 16-bit register
        public boolean isDoubleRegister() {
            switch (this) {
                case A:
                case B:
                case C:
                case D:
                case E:
                case F:
                case H:
                case L:
                case HL_ADDRESS:
                    return false;
                default:
                    return true;
            }
        }
    }

    // Set the value of an 8-bit register
    public void setRegister(Register register, byte value) {
        if (register.isDoubleRegister()) {
            throw new IllegalArgumentException("Must pass 8-bit register");
        }
        switch (register) {
            case A:
                this.A = value;
                break;
            case B:
                this.B = value;
                break;
            case C:
                this.C = value;
                break;
            case D:
                this.D = value;
                break;
            case E:
                this.E = value;
                break;
            case F:
                this.F = value;
                break;
            case H:
                this.H = value;
                break;
            case L:
                this.L = value;
                break;
            case HL_ADDRESS:
                throw new IllegalArgumentException("Cannot set HL_ADDRESS "
                        + "directly, must set HL or memory, depending on context");
            default:
                throw new IllegalArgumentException("Invalid 8-bit register passed");
        }

    }

    // Set the value of a 16-bit register
    public void setRegister(Register register, byte firstByte, byte secondByte) {
        if (!register.isDoubleRegister()) {
            throw new IllegalArgumentException("Must pass 16-bit register if passing in two bytes!");
        }
        switch (register) {
            case BC:
                this.B = firstByte;
                this.C = secondByte;
                break;
            case DE:
                this.D = firstByte;
                this.E = secondByte;
                break;
            case AF:
                this.A = firstByte;
                this.F = secondByte;
                break;
            case HL:
                this.H = firstByte;
                this.L = secondByte;
                break;
            case SP:
                this.SP = Util.concatBytes(firstByte, secondByte);
                break;
            case PC:
                this.PC = Util.concatBytes(firstByte, secondByte);
                break;
            default:
                throw new RuntimeException("Invalid register passed");
        }

    }

    // Get the value stored in a given 8-bit register as a byte
    public byte get8BitRegisterValue(Register r) {
        switch (r) {
            case A:
                return A;
            case B:
                return B;
            case C:
                return C;
            case D:
                return D;
            case E:
                return E;
            case F:
                return F;
            case H:
                return H;
            case L:
                return L;
            case HL_ADDRESS:
                throw new IllegalArgumentException("CPU must call getByteFromHL!");
            default:
                throw new RuntimeException("Register provided did not match known 8-bit register");
        }
    }

    // Get the value stored in a given 16-bit register as a short
    //
    // Note: This is an UNSIGNED 16-bit number stored in a short! It must be
    // converted to an int be used as an address
    public short get16BitRegisterValue(Register rr) {
        switch (rr) {
            case SP:
                return SP;
            case PC:
                return PC;
            case HL:
                return Util.concatBytes(H, L);
            case DE:
                return Util.concatBytes(D, E);
            case BC:
                return Util.concatBytes(B, C);
            case AF:
                return Util.concatBytes(A, F);
            default:
                throw new IllegalArgumentException("Register provided did not "
                        + "match known 8-bit register");
        }
    }

    // Set a short value to a 16-bit register
    public void set16BitRegister(Register RR, short s) {
        setRegister(RR, Util.splitShortToBytes(s)[0], Util.splitShortToBytes(s)[1]);
    }
}

package mattpvaughn.io.github.emulator.cpu;

public class UnknownOperationException extends IllegalArgumentException {
    public UnknownOperationException(String errorMessage) {
        super(errorMessage);
    }
}

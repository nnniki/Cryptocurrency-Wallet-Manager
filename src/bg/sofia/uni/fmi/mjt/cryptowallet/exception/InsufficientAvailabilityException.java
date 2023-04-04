package bg.sofia.uni.fmi.mjt.cryptowallet.exception;

public class InsufficientAvailabilityException extends Exception {
    public InsufficientAvailabilityException(String message) {
        super(message);
    }

    public InsufficientAvailabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}

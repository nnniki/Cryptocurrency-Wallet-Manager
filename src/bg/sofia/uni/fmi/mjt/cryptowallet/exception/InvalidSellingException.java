package bg.sofia.uni.fmi.mjt.cryptowallet.exception;

public class InvalidSellingException extends Exception {
    public InvalidSellingException(String message) {
        super(message);
    }

    public InvalidSellingException(String message, Throwable cause) {
        super(message, cause);
    }
}

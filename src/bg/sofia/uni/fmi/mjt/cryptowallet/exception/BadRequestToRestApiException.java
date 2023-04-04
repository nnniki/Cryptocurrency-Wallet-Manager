package bg.sofia.uni.fmi.mjt.cryptowallet.exception;

public class BadRequestToRestApiException extends Exception {
    public BadRequestToRestApiException(String message) {
        super(message);
    }

    public BadRequestToRestApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

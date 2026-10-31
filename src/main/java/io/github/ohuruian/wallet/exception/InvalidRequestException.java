package io.github.ohuruian.wallet.exception;

/** A client error whose message is safe to return in the API response. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

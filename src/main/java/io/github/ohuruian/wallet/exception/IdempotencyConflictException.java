package io.github.ohuruian.wallet.exception;

/** The same Idempotency-Key was reused for a request that does not match the original one. */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency-Key was already used for a different request");
    }
}

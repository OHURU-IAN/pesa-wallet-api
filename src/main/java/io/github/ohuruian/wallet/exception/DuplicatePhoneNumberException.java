package io.github.ohuruian.wallet.exception;

/** A wallet already exists for the given phone number. The number is deliberately not echoed back. */
public class DuplicatePhoneNumberException extends RuntimeException {

    public DuplicatePhoneNumberException() {
        super("A wallet already exists for this phone number");
    }
}

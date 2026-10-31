package io.github.ohuruian.wallet.exception;

import java.math.BigDecimal;

/** Thrown when a debit would take a wallet balance below zero. */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
        super("Insufficient funds: available=" + available.toPlainString()
                + ", requested=" + requested.toPlainString());
    }
}

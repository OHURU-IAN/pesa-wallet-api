package io.github.ohuruian.wallet.web.dto;

import io.github.ohuruian.wallet.service.TransferResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Returned to the sender. The receiver's balance is deliberately not exposed. */
public record TransferResponse(String reference, UUID fromWalletId, UUID toWalletId, BigDecimal amount,
                               BigDecimal fromBalanceAfter, Instant createdAt) {

    public static TransferResponse from(TransferResult result) {
        var out = result.outgoing();
        return new TransferResponse(out.getReference(), out.getWalletId(), out.getCounterpartyWalletId(),
                out.getAmount(), out.getBalanceAfter(), out.getCreatedAt());
    }
}

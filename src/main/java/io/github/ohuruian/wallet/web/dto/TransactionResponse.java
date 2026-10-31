package io.github.ohuruian.wallet.web.dto;

import io.github.ohuruian.wallet.domain.Transaction;
import io.github.ohuruian.wallet.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(UUID id, UUID walletId, TransactionType type, BigDecimal amount,
                                  BigDecimal balanceAfter, UUID counterpartyWalletId, String reference,
                                  Instant createdAt) {

    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(tx.getId(), tx.getWalletId(), tx.getType(), tx.getAmount(),
                tx.getBalanceAfter(), tx.getCounterpartyWalletId(), tx.getReference(), tx.getCreatedAt());
    }
}

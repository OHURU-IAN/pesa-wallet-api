package io.github.ohuruian.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * An immutable ledger entry. A transfer produces two entries (TRANSFER_OUT and TRANSFER_IN)
 * that share the same {@code reference}.
 */
@Entity
@Table(name = "wallet_transaction", indexes = {
        @Index(name = "idx_tx_wallet_created", columnList = "walletId, createdAt")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = Wallet.MONEY_SCALE)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = Wallet.MONEY_SCALE)
    private BigDecimal balanceAfter;

    private UUID counterpartyWalletId;

    @Column(nullable = false, length = 36)
    private String reference;

    @Column(unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Transaction() {
        // for JPA
    }

    private Transaction(UUID walletId, TransactionType type, BigDecimal amount, BigDecimal balanceAfter,
                        UUID counterpartyWalletId, String reference, String idempotencyKey) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.counterpartyWalletId = counterpartyWalletId;
        this.reference = reference;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public static Transaction single(Wallet wallet, TransactionType type, BigDecimal amount, String idempotencyKey) {
        return new Transaction(wallet.getId(), type, amount, wallet.getBalance(), null,
                UUID.randomUUID().toString(), idempotencyKey);
    }

    public static Transaction transferLeg(Wallet wallet, TransactionType type, BigDecimal amount,
                                          UUID counterpartyWalletId, String reference) {
        return new Transaction(wallet.getId(), type, amount, wallet.getBalance(), counterpartyWalletId,
                reference, null);
    }

    /** True when a replayed request carries the same operation as this stored one. */
    public boolean matches(UUID otherWalletId, TransactionType otherType, BigDecimal otherAmount) {
        return walletId.equals(otherWalletId) && type == otherType && amount.compareTo(otherAmount) == 0;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public UUID getCounterpartyWalletId() {
        return counterpartyWalletId;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

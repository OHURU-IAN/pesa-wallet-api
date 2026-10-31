package io.github.ohuruian.wallet.web.dto;

import io.github.ohuruian.wallet.domain.Wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(UUID id, String ownerName, String phoneNumber, String currency,
                             BigDecimal balance, Instant createdAt) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getOwnerName(), wallet.getPhoneNumber(),
                wallet.getCurrency(), wallet.getBalance(), wallet.getCreatedAt());
    }
}

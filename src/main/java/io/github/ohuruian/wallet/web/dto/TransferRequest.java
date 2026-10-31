package io.github.ohuruian.wallet.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID fromWalletId,
        @NotNull UUID toWalletId,
        @NotNull
        @DecimalMin(value = "1.00", message = "must be at least 1.00")
        @DecimalMax(value = "150000.00", message = "must not exceed 150000.00 per transaction")
        @Digits(integer = 6, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount) {
}

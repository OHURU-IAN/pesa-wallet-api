package io.github.ohuruian.wallet.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Amount in KES: at least 1.00, at most 150,000.00 per transaction, max two decimal places. */
public record AmountRequest(
        @Schema(example = "2500.00")
        @NotNull
        @DecimalMin(value = "1.00", message = "must be at least 1.00")
        @DecimalMax(value = "150000.00", message = "must not exceed 150000.00 per transaction")
        @Digits(integer = 6, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount) {
}

package io.github.ohuruian.wallet.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWalletRequest(
        @Schema(example = "Ian Ohuru")
        @NotBlank @Size(max = 100)
        String ownerName,

        @Schema(example = "254712345678", description = "Kenyan mobile number in international format")
        @NotBlank
        @Pattern(regexp = "^254[71]\\d{8}$",
                message = "must be a Kenyan mobile number in the format 2547XXXXXXXX or 2541XXXXXXXX")
        String phoneNumber) {
}

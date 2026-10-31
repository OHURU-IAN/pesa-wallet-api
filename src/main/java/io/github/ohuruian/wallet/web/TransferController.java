package io.github.ohuruian.wallet.web;

import io.github.ohuruian.wallet.service.WalletService;
import io.github.ohuruian.wallet.web.dto.TransferRequest;
import io.github.ohuruian.wallet.web.dto.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Transfers", description = "Move money between wallets")
public class TransferController {

    private final WalletService walletService;

    public TransferController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer money between two wallets atomically")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return TransferResponse.from(
                walletService.transfer(request.fromWalletId(), request.toWalletId(), request.amount()));
    }
}

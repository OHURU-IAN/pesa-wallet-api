package io.github.ohuruian.wallet.web;

import io.github.ohuruian.wallet.exception.InvalidRequestException;
import io.github.ohuruian.wallet.service.MovementResult;
import io.github.ohuruian.wallet.service.WalletService;
import io.github.ohuruian.wallet.web.dto.AmountRequest;
import io.github.ohuruian.wallet.web.dto.CreateWalletRequest;
import io.github.ohuruian.wallet.web.dto.PageResponse;
import io.github.ohuruian.wallet.web.dto.TransactionResponse;
import io.github.ohuruian.wallet.web.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@Tag(name = "Wallets", description = "Open wallets, deposit, withdraw and view history")
public class WalletController {

    static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @Operation(summary = "Open a wallet for a Kenyan mobile number")
    public ResponseEntity<WalletResponse> create(@Valid @RequestBody CreateWalletRequest request) {
        var wallet = walletService.createWallet(request.ownerName(), request.phoneNumber());
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(wallet.getId()).toUri();
        return ResponseEntity.created(location).body(WalletResponse.from(wallet));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a wallet and its current balance")
    public WalletResponse get(@PathVariable UUID id) {
        return WalletResponse.from(walletService.getWallet(id));
    }

    @PostMapping("/{id}/deposits")
    @Operation(summary = "Deposit money", description = "Send an Idempotency-Key header to make retries safe")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable UUID id,
            @Valid @RequestBody AmountRequest request,
            @Parameter(description = "Unique key per logical request; retries with the same key are not charged twice")
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        return toResponse(walletService.deposit(id, request.amount(), validKey(idempotencyKey)));
    }

    @PostMapping("/{id}/withdrawals")
    @Operation(summary = "Withdraw money", description = "Rejected with 422 if the balance is too low")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable UUID id,
            @Valid @RequestBody AmountRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        return toResponse(walletService.withdraw(id, request.amount(), validKey(idempotencyKey)));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Transaction history, newest first")
    public PageResponse<TransactionResponse> transactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return PageResponse.from(walletService.listTransactions(id, pageable), TransactionResponse::from);
    }

    private static ResponseEntity<TransactionResponse> toResponse(MovementResult result) {
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(TransactionResponse.from(result.transaction()));
    }

    private static String validKey(String key) {
        if (key == null) {
            return null;
        }
        if (key.isBlank() || key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new InvalidRequestException(
                    IDEMPOTENCY_HEADER + " must be 1-" + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }
        return key;
    }
}

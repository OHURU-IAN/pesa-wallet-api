package io.github.ohuruian.wallet.service;

import io.github.ohuruian.wallet.domain.Transaction;
import io.github.ohuruian.wallet.domain.TransactionType;
import io.github.ohuruian.wallet.domain.Wallet;
import io.github.ohuruian.wallet.exception.DuplicatePhoneNumberException;
import io.github.ohuruian.wallet.exception.IdempotencyConflictException;
import io.github.ohuruian.wallet.exception.InvalidRequestException;
import io.github.ohuruian.wallet.exception.InvalidTransferException;
import io.github.ohuruian.wallet.exception.WalletNotFoundException;
import io.github.ohuruian.wallet.repository.TransactionRepository;
import io.github.ohuruian.wallet.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class WalletService {

    private static final int MONEY_SCALE = 2;

    private final WalletRepository wallets;
    private final TransactionRepository transactions;

    public WalletService(WalletRepository wallets, TransactionRepository transactions) {
        this.wallets = wallets;
        this.transactions = transactions;
    }

    public Wallet createWallet(String ownerName, String phoneNumber) {
        if (wallets.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicatePhoneNumberException();
        }
        return wallets.save(new Wallet(ownerName.strip(), phoneNumber));
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId) {
        return wallets.findById(walletId).orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> listTransactions(UUID walletId, Pageable pageable) {
        if (!wallets.existsById(walletId)) {
            throw new WalletNotFoundException(walletId);
        }
        return transactions.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
    }

    public MovementResult deposit(UUID walletId, BigDecimal amount, String idempotencyKey) {
        return move(walletId, TransactionType.DEPOSIT, amount, idempotencyKey);
    }

    public MovementResult withdraw(UUID walletId, BigDecimal amount, String idempotencyKey) {
        return move(walletId, TransactionType.WITHDRAWAL, amount, idempotencyKey);
    }

    /**
     * Moves money between two wallets atomically. Both rows are locked in a fixed order
     * (lowest id first) so two opposite transfers can never deadlock each other.
     */
    public TransferResult transfer(UUID fromWalletId, UUID toWalletId, BigDecimal requestedAmount) {
        BigDecimal amount = toMoney(requestedAmount);
        if (fromWalletId.equals(toWalletId)) {
            throw new InvalidTransferException("Cannot transfer to the same wallet");
        }
        boolean fromFirst = fromWalletId.compareTo(toWalletId) < 0;
        Wallet firstLocked = lock(fromFirst ? fromWalletId : toWalletId);
        Wallet secondLocked = lock(fromFirst ? toWalletId : fromWalletId);
        Wallet from = fromFirst ? firstLocked : secondLocked;
        Wallet to = fromFirst ? secondLocked : firstLocked;

        from.debit(amount);
        to.credit(amount);

        String reference = UUID.randomUUID().toString();
        Transaction outgoing = transactions.save(
                Transaction.transferLeg(from, TransactionType.TRANSFER_OUT, amount, to.getId(), reference));
        Transaction incoming = transactions.save(
                Transaction.transferLeg(to, TransactionType.TRANSFER_IN, amount, from.getId(), reference));
        return new TransferResult(outgoing, incoming);
    }

    private MovementResult move(UUID walletId, TransactionType type, BigDecimal requestedAmount,
                                String idempotencyKey) {
        BigDecimal amount = toMoney(requestedAmount);
        Optional<MovementResult> replay = findReplay(walletId, type, amount, idempotencyKey);
        if (replay.isPresent()) {
            return replay.get();
        }

        Wallet wallet = lock(walletId);
        if (type == TransactionType.DEPOSIT) {
            wallet.credit(amount);
        } else {
            wallet.debit(amount);
        }
        Transaction saved = transactions.save(Transaction.single(wallet, type, amount, idempotencyKey));
        return new MovementResult(saved, false);
    }

    private Optional<MovementResult> findReplay(UUID walletId, TransactionType type, BigDecimal amount,
                                                String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return transactions.findByIdempotencyKey(idempotencyKey).map(existing -> {
            if (!existing.matches(walletId, type, amount)) {
                throw new IdempotencyConflictException();
            }
            return new MovementResult(existing, true);
        });
    }

    /** Normalises to two decimal places; rejects amounts with more precision instead of rounding money. */
    private static BigDecimal toMoney(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidRequestException("Amount is required");
        }
        try {
            return amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new InvalidRequestException("Amount must have at most 2 decimal places");
        }
    }

    private Wallet lock(UUID walletId) {
        return wallets.findByIdForUpdate(walletId).orElseThrow(() -> new WalletNotFoundException(walletId));
    }
}

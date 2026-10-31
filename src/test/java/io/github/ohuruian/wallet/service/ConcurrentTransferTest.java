package io.github.ohuruian.wallet.service;

import io.github.ohuruian.wallet.domain.Wallet;
import io.github.ohuruian.wallet.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires many transfers in both directions at the same time. Without row locking this
 * either loses updates (money appears or disappears) or deadlocks.
 */
@SpringBootTest
class ConcurrentTransferTest {

    private static final int TRANSFERS_PER_DIRECTION = 40;

    @Autowired
    private WalletService walletService;

    @Test
    void concurrentTransfersInBothDirectionsConserveTotalBalance() throws Exception {
        Wallet a = walletService.createWallet("Wallet A", "254700000001");
        Wallet b = walletService.createWallet("Wallet B", "254700000002");
        walletService.deposit(a.getId(), new BigDecimal("1000.00"), null);
        walletService.deposit(b.getId(), new BigDecimal("1000.00"), null);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < TRANSFERS_PER_DIRECTION; i++) {
            tasks.add(transfer(a.getId(), b.getId(), "10.00"));
            tasks.add(transfer(b.getId(), a.getId(), "7.50"));
        }

        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (Future<Void> f : pool.invokeAll(tasks)) {
                f.get();
            }
        }

        BigDecimal balanceA = walletService.getWallet(a.getId()).getBalance();
        BigDecimal balanceB = walletService.getWallet(b.getId()).getBalance();

        assertThat(balanceA.add(balanceB)).isEqualByComparingTo("2000.00");
        assertThat(balanceA).isEqualByComparingTo("900.00");   // 1000 - 40*10 + 40*7.50
        assertThat(balanceB).isEqualByComparingTo("1100.00");
    }

    private Callable<Void> transfer(UUID from, UUID to, String amount) {
        return () -> {
            try {
                walletService.transfer(from, to, new BigDecimal(amount));
            } catch (InsufficientFundsException ignored) {
                // not expected with these amounts, but must never corrupt balances
            }
            return null;
        };
    }
}

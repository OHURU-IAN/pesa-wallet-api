package io.github.ohuruian.wallet.domain;

import io.github.ohuruian.wallet.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    @Test
    void newWalletStartsWithZeroBalanceInKes() {
        Wallet wallet = new Wallet("Ian Ohuru", "254712345678");

        assertThat(wallet.getBalance()).isEqualByComparingTo("0.00");
        assertThat(wallet.getCurrency()).isEqualTo("KES");
    }

    @Test
    void creditAddsToBalance() {
        Wallet wallet = new Wallet("Ian Ohuru", "254712345678");

        wallet.credit(new BigDecimal("1500.50"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("1500.50");
    }

    @Test
    void debitSubtractsFromBalance() {
        Wallet wallet = new Wallet("Ian Ohuru", "254712345678");
        wallet.credit(new BigDecimal("1000.00"));

        wallet.debit(new BigDecimal("400.25"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("599.75");
    }

    @Test
    void debitMoreThanBalanceThrowsAndLeavesBalanceUnchanged() {
        Wallet wallet = new Wallet("Ian Ohuru", "254712345678");
        wallet.credit(new BigDecimal("100.00"));

        assertThatThrownBy(() -> wallet.debit(new BigDecimal("100.01")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        Wallet wallet = new Wallet("Ian Ohuru", "254712345678");

        assertThatThrownBy(() -> wallet.credit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.debit(new BigDecimal("-5"))).isInstanceOf(IllegalArgumentException.class);
    }
}

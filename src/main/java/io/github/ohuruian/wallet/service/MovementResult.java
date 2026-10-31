package io.github.ohuruian.wallet.service;

import io.github.ohuruian.wallet.domain.Transaction;

/**
 * Outcome of a deposit or withdrawal.
 *
 * @param transaction the ledger entry
 * @param replayed    true when an earlier request with the same Idempotency-Key was returned instead
 */
public record MovementResult(Transaction transaction, boolean replayed) {
}

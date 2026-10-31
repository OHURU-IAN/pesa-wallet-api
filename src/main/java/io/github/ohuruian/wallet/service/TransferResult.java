package io.github.ohuruian.wallet.service;

import io.github.ohuruian.wallet.domain.Transaction;

/** Both ledger entries written by one transfer. They share the same reference. */
public record TransferResult(Transaction outgoing, Transaction incoming) {
}

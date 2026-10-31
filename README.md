# Pesa Wallet API

A mobile-wallet REST API built with **Java 21** and **Spring Boot 4**. It models KES wallets tied to
Kenyan phone numbers, with deposits, withdrawals, atomic wallet-to-wallet transfers and a paged
transaction ledger. The focus is on getting money handling right: exact decimal arithmetic,
no double-charging on retries, and correct balances under concurrent load.

## Features

- **Wallets** for Kenyan mobile numbers (`2547XXXXXXXX` / `2541XXXXXXXX`), one per number
- **Deposits and withdrawals** with per-transaction limits (KES 1.00 – 150,000.00, max 2 decimals)
- **Atomic transfers** between wallets, recorded as two linked ledger entries sharing one reference
- **Idempotency keys**: retrying a deposit/withdrawal with the same `Idempotency-Key` header returns
  the original result instead of charging again; reusing a key for a different request is a `409`
- **Transaction history**, newest first, paginated
- **Consistent errors** as RFC 9457 problem details (`application/problem+json`)
- **OpenAPI / Swagger UI** documentation generated from the code

## Design decisions

| Concern | Approach |
|---|---|
| Money precision | `BigDecimal` with scale 2 everywhere; amounts with more precision are rejected, never rounded |
| Lost updates | Each balance change locks the wallet row (`SELECT … FOR UPDATE`) inside a transaction |
| Deadlocks | Transfers lock both wallets in a fixed order (lowest id first), so opposite transfers can't deadlock |
| Double charging | Optional `Idempotency-Key` stored with a unique constraint on the ledger entry |
| Invariants | The `Wallet` entity is the only place balances change, and it refuses to go negative |
| Privacy | Errors never echo phone numbers; the transfer response doesn't reveal the receiver's balance |

A concurrency test fires 80 transfers in both directions at the same time and asserts the total
money in the system is unchanged and both balances are exact.

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/wallets` | Open a wallet |
| `GET` | `/api/wallets/{id}` | Wallet details and balance |
| `POST` | `/api/wallets/{id}/deposits` | Deposit (optional `Idempotency-Key` header) |
| `POST` | `/api/wallets/{id}/withdrawals` | Withdraw (`422` if funds are insufficient) |
| `POST` | `/api/transfers` | Transfer between two wallets |
| `GET` | `/api/wallets/{id}/transactions?page=0&size=20` | Ledger, newest first |

Interactive docs: <http://localhost:8080/swagger-ui.html> · OpenAPI JSON: `/v3/api-docs` · Health: `/actuator/health`

### Example

```bash
# Open a wallet
curl -X POST localhost:8080/api/wallets -H "Content-Type: application/json" \
  -d '{"ownerName":"Ian Ohuru","phoneNumber":"254712345678"}'

# Deposit safely (a retry with the same key is not charged twice)
curl -X POST localhost:8080/api/wallets/{id}/deposits -H "Content-Type: application/json" \
  -H "Idempotency-Key: 7f3c-deposit-001" -d '{"amount":5000}'

# Transfer
curl -X POST localhost:8080/api/transfers -H "Content-Type: application/json" \
  -d '{"fromWalletId":"{id}","toWalletId":"{otherId}","amount":1250.50}'
```

Error example (`422`):

```json
{
  "title": "Insufficient funds",
  "status": 422,
  "detail": "Insufficient funds: available=1250.50, requested=99999",
  "instance": "/api/wallets/18dd80ea-.../withdrawals"
}
```

## Running locally

Requires **JDK 21+**. No database setup is needed: it uses in-memory H2 by default.

```bash
./mvnw spring-boot:run          # macOS / Linux
mvnw.cmd spring-boot:run        # Windows
```

### With PostgreSQL

```bash
export DB_URL=jdbc:postgresql://localhost:5432/wallet
export DB_USERNAME=wallet
export DB_PASSWORD=your-password
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test
```

20 tests covering the domain rules, every endpoint (validation, 404/409/422 paths, idempotent
replays, pagination) and concurrent transfers. Line coverage is about 90%; the JaCoCo report is
written to `target/site/jacoco/index.html`.

## Tech stack

Java 21 · Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator) · Hibernate 7 · H2 / PostgreSQL ·
springdoc-openapi · JUnit 5 · AssertJ · MockMvc · JaCoCo

## Possible next steps

- Authentication (Spring Security + JWT) so users can only act on their own wallets
- Schema migrations with Flyway instead of Hibernate auto-DDL
- An M-Pesa Daraja (STK Push) adapter for real deposits
- Docker image and a GitHub Actions CI pipeline

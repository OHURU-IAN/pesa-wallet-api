package io.github.ohuruian.wallet.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletApiIntegrationTest {

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(10_000_000);

    @Autowired
    private MockMvc mvc;

    @Test
    void createsWalletWithZeroBalance() throws Exception {
        String phone = uniquePhone();

        mvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content(walletJson("Ian Ohuru", phone)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.phoneNumber").value(phone))
                .andExpect(jsonPath("$.currency").value("KES"))
                .andExpect(jsonPath("$.balance").value(0.00));
    }

    @Test
    void rejectsInvalidKenyanPhoneNumberWithFieldError() throws Exception {
        mvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content(walletJson("Ian Ohuru", "0712345678")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.phoneNumber").isNotEmpty());
    }

    @Test
    void rejectsDuplicatePhoneNumber() throws Exception {
        String phone = uniquePhone();
        createWallet(phone);

        mvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content(walletJson("Someone Else", phone)))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownWalletReturns404ProblemDetail() throws Exception {
        mvc.perform(get("/api/wallets/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Wallet not found"));
    }

    @Test
    void depositIncreasesBalanceAndRecordsTransaction() throws Exception {
        String walletId = createWallet(uniquePhone());

        deposit(walletId, "2500.00", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(2500.00))
                .andExpect(jsonPath("$.balanceAfter").value(2500.00));

        mvc.perform(get("/api/wallets/" + walletId))
                .andExpect(jsonPath("$.balance").value(2500.00));
    }

    @Test
    void wholeNumberAmountsAreReturnedWithTwoDecimalPlaces() throws Exception {
        String walletId = createWallet(uniquePhone());

        String body = deposit(walletId, "5000", null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body).contains("\"amount\":5000.00");
    }

    @Test
    void rejectsAmountsWithMoreThanTwoDecimalPlacesOrAboveLimit() throws Exception {
        String walletId = createWallet(uniquePhone());

        deposit(walletId, "10.005", null).andExpect(status().isBadRequest());
        deposit(walletId, "150000.01", null).andExpect(status().isBadRequest());
        deposit(walletId, "0", null).andExpect(status().isBadRequest());
    }

    @Test
    void withdrawalBeyondBalanceIsRejectedWith422() throws Exception {
        String walletId = createWallet(uniquePhone());
        deposit(walletId, "100.00", null);

        mvc.perform(post("/api/wallets/" + walletId + "/withdrawals").contentType(MediaType.APPLICATION_JSON)
                        .content(amountJson("100.01")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Insufficient funds"));

        mvc.perform(get("/api/wallets/" + walletId))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void transferMovesMoneyBetweenWallets() throws Exception {
        String from = createWallet(uniquePhone());
        String to = createWallet(uniquePhone());
        deposit(from, "1000.00", null);

        mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(from, to, "350.50")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.fromBalanceAfter").value(649.50));

        mvc.perform(get("/api/wallets/" + from)).andExpect(jsonPath("$.balance").value(649.50));
        mvc.perform(get("/api/wallets/" + to)).andExpect(jsonPath("$.balance").value(350.50));
    }

    @Test
    void transferToSameWalletIsRejected() throws Exception {
        String wallet = createWallet(uniquePhone());
        deposit(wallet, "100.00", null);

        mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(wallet, wallet, "10.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replayingDepositWithSameIdempotencyKeyDoesNotChargeTwice() throws Exception {
        String walletId = createWallet(uniquePhone());

        String firstTxId = JsonPath.read(deposit(walletId, "500.00", "key-abc-" + walletId)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        deposit(walletId, "500.00", "key-abc-" + walletId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstTxId));

        mvc.perform(get("/api/wallets/" + walletId)).andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void reusingIdempotencyKeyForDifferentRequestIsAConflict() throws Exception {
        String walletId = createWallet(uniquePhone());
        deposit(walletId, "500.00", "key-xyz-" + walletId).andExpect(status().isCreated());

        deposit(walletId, "999.00", "key-xyz-" + walletId)
                .andExpect(status().isConflict());
    }

    @Test
    void listsTransactionsNewestFirstWithPagination() throws Exception {
        String walletId = createWallet(uniquePhone());
        deposit(walletId, "100.00", null);
        deposit(walletId, "200.00", null);
        deposit(walletId, "300.00", null);

        mvc.perform(get("/api/wallets/" + walletId + "/transactions").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].amount").value(300.00))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    // ---- helpers ----

    private String createWallet(String phone) throws Exception {
        String body = mvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content(walletJson("Test User", phone)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions deposit(String walletId, String amount, String idempotencyKey) throws Exception {
        var request = post("/api/wallets/" + walletId + "/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(amountJson(amount));
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mvc.perform(request);
    }

    private static String uniquePhone() {
        return "2547" + PHONE_SEQ.incrementAndGet();
    }

    private static String walletJson(String owner, String phone) {
        return """
                {"ownerName": "%s", "phoneNumber": "%s"}""".formatted(owner, phone);
    }

    private static String amountJson(String amount) {
        return """
                {"amount": %s}""".formatted(amount);
    }

    private static String transferJson(String from, String to, String amount) {
        return """
                {"fromWalletId": "%s", "toWalletId": "%s", "amount": %s}""".formatted(from, to, amount);
    }
}

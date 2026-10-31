package io.github.ohuruian.wallet;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Pesa Wallet API",
        version = "1.0",
        description = "Mobile wallet REST API: KES wallets, deposits, withdrawals and atomic transfers"))
public class PesaWalletApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PesaWalletApiApplication.class, args);
    }
}

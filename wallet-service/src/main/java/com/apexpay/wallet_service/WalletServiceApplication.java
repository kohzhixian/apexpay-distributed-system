package com.apexpay.wallet_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main application class for the Wallet Service.
 * <p>
 * This microservice handles wallet operations including:
 * <ul>
 * <li>Wallet creation and management</li>
 * <li>Balance top-ups and transfers</li>
 * <li>Fund reservations for payment processing</li>
 * <li>Transaction history tracking</li>
 * </ul>
 * </p>
 * <p>
 * The service uses optimistic locking with retry mechanisms to handle
 * concurrent updates and integrates with the payment service for two-phase
 * commit payment processing.
 * </p>
 */
@SpringBootApplication
@EnableFeignClients
@EnableRetry
public class WalletServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }

}

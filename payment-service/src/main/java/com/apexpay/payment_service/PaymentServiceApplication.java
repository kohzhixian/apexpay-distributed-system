package com.apexpay.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for the Payment Service.
 * <p>
 * This microservice handles payment processing operations including:
 * <ul>
 * <li>Payment initiation and processing</li>
 * <li>Integration with external payment providers</li>
 * <li>Coordination with wallet service for fund reservations</li>
 * </ul>
 * </p>
 * <p>
 * The service uses Feign clients to communicate with other microservices
 * in the ApexPay distributed system.
 * </p>
 */
@SpringBootApplication
@EnableFeignClients
public class PaymentServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}

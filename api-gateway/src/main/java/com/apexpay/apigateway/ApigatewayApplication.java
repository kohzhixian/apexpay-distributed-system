package com.apexpay.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the API Gateway.
 * <p>
 * Acts as the single entry point for all client requests.
 * Handles JWT authentication, request routing to microservices,
 * and circuit breaker patterns for resilience.
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApigatewayApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(ApigatewayApplication.class, args);
	}

}

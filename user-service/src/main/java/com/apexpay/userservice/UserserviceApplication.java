package com.apexpay.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the User Service.
 * <p>
 * Handles user registration, authentication, JWT token management,
 * and user profile operations. Registers with Eureka for service discovery.
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient // Registers microservice with Eureka Server
public class UserserviceApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(UserserviceApplication.class, args);
	}

}

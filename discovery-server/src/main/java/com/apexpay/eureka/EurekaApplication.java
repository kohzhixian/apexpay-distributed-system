package com.apexpay.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Main application class for the Eureka Discovery Server.
 * <p>
 * Provides service discovery and registration for all microservices
 * in the ApexPay distributed system. Services register themselves
 * with Eureka and use it to discover other services.
 * </p>
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(EurekaApplication.class, args);
	}

}

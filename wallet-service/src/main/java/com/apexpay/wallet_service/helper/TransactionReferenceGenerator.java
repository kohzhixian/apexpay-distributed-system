package com.apexpay.wallet_service.helper;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates human-readable transaction references for customer support.
 * <p>
 * Format: APX-XXXX-XXX-XXX where:
 * <ul>
 *   <li>APX - ApexPay prefix</li>
 *   <li>XXXX - 4 random digits</li>
 *   <li>XXX - 3 random uppercase letters</li>
 *   <li>XXX - 3 random digits</li>
 * </ul>
 * Example: APX-8921-MNQ-772
 * </p>
 */
@Component
public class TransactionReferenceGenerator {

    private static final String PREFIX = "APX";
    private static final String DIGITS = "0123456789";
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // Excludes I and O to avoid confusion
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a unique human-readable transaction reference.
     *
     * @return a transaction reference in format APX-XXXX-XXX-XXX
     */
    public String generate() {
        return String.format("%s-%s-%s-%s",
                PREFIX,
                randomDigits(4),
                randomLetters(3),
                randomDigits(3));
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }

    private String randomLetters(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }
        return sb.toString();
    }
}

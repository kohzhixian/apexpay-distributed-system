package com.apexpay.payment_service.controller;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.common.dto.PaymentResponse;
import com.apexpay.payment_service.dto.InitiatePaymentRequest;
import com.apexpay.payment_service.dto.InitiatePaymentResponse;
import com.apexpay.payment_service.dto.ProcessPaymentRequest;
import com.apexpay.payment_service.service.PaymentService;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for payment operations.
 * <p>
 * Handles payment initiation and processing requests. All endpoints require
 * authentication via the X-USER-ID header.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {
    private final PaymentService paymentService;

    /**
     * Constructs a new PaymentController with the specified payment service.
     *
     * @param paymentService the payment service to handle business logic
     */
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Initiates a new payment request.
     * <p>
     * Creates a payment record in INITIATED status. This endpoint is idempotent -
     * if a payment with the same clientRequestId already exists for the user,
     * returns the existing payment instead of creating a duplicate.
     * </p>
     *
     * @param request the payment initiation request containing amount, currency,
     *                walletId, clientRequestId, and provider
     * @param userId  the authenticated user's ID from the X-USER-ID header
     * @return response containing paymentId and version for subsequent processing
     */
    @PostMapping
    public ResponseEntity<InitiatePaymentResponse> initiatePayment(InitiatePaymentRequest request,
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        InitiatePaymentResponse response = paymentService.initiatePayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Processes an initiated payment through the full payment flow.
     * <p>
     * Executes the two-phase commit pattern:
     * <ol>
     * <li>Reserves funds in the user's wallet</li>
     * <li>Charges the external payment provider</li>
     * <li>Confirms or cancels the reservation based on charge result</li>
     * </ol>
     * </p>
     *
     * @param paymentId the ID of the payment to process
     * @param request   the process payment request containing paymentMethodToken
     * @param userId    the authenticated user's ID from the X-USER-ID header
     * @return response with final payment status (SUCCESS, PENDING, or FAILED)
     */
    @PostMapping("/{paymentId}/process")
    public ResponseEntity<PaymentResponse> processPayment(@PathVariable("paymentId") UUID paymentId,
            @RequestBody ProcessPaymentRequest request, @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        PaymentResponse response = paymentService.processPayment(paymentId, userId, request.paymentMethodToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Checks the status of a pending payment with the payment provider.
     * <p>
     * Used to poll for status updates when a payment is in PENDING state.
     * Queries the payment provider for the current transaction status and
     * updates the payment accordingly. If the payment is not in PENDING status,
     * returns the current status without querying the provider.
     * </p>
     *
     * @param paymentId the ID of the payment to check
     * @param userId     the authenticated user's ID from the X-USER-ID header
     * @return response with current payment status
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<PaymentResponse> checkPaymentStatus(@PathVariable("paymentId") UUID paymentId,
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        PaymentResponse response = paymentService.checkPaymentStatus(paymentId, userId);
        return ResponseEntity.ok(response);
    }
}

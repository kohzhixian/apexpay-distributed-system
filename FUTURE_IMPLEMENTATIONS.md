# Future Implementations

This document outlines scheduled jobs and background processes to be implemented for system maintenance, reconciliation, and data consistency.

---

## 1. Payment Expiration Job

**Purpose:** Expire INITIATED payments that were never processed within the allowed time window.

**Why needed:**
- Users may initiate payments but never complete them (abandon checkout, network issues, etc.)
- Orphaned INITIATED records accumulate in the database
- Expired payments should allow the same `clientRequestId` to be reused for a new payment

**Implementation Details:**

```java
@Component
public class PaymentExpirationJob {
    
    private static final int EXPIRATION_MINUTES = 30; // Configurable
    
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void expireStalePayments() {
        // 1. Find all payments WHERE:
        //    - status = 'INITIATED'
        //    - createdAt < (now - EXPIRATION_MINUTES)
        
        // 2. Update status to 'EXPIRED' for all matching records
        
        // 3. Log the number of expired payments for monitoring
    }
}
```

**Repository method needed:**

```java
@Modifying
@Query("UPDATE Payments p SET p.status = 'EXPIRED', p.version = p.version + 1 " +
       "WHERE p.status = 'INITIATED' AND p.createdAt < :cutoffTime")
int expireStalePayments(@Param("cutoffTime") Instant cutoffTime);
```

**Configuration (application.yaml):**

```yaml
payment:
  expiration:
    enabled: true
    minutes: 30
    job-interval-ms: 300000  # 5 minutes
```

---

## 2. Wallet-Payment Reconciliation Job

**Purpose:** Detect and resolve inconsistencies between wallet transactions and payment records.

**Scenarios to handle:**

### 2a. Zombie Reservations
- **Condition:** Wallet has a RESERVED transaction, but payment is SUCCESS/FAILED
- **Cause:** `confirmReservation` or `cancelReservation` HTTP call failed after payment provider responded
- **Resolution:**
  - If payment is SUCCESS → Call `confirmReservation` to complete the wallet transaction
  - If payment is FAILED → Call `cancelReservation` to release the reserved funds

### 2b. Stuck PENDING Payments
- **Condition:** Payment is in PENDING status for too long (e.g., > 1 hour)
- **Cause:** Status polling was never completed, provider callback missed
- **Resolution:**
  - Query payment provider for current status
  - Update payment and wallet accordingly

### 2c. Orphaned Wallet Reservations
- **Condition:** Wallet has RESERVED transaction with no matching payment record
- **Cause:** Payment record creation failed after wallet reservation
- **Resolution:** Cancel the orphaned reservation

**Implementation Details:**

```java
@Component
public class WalletPaymentReconciliationJob {
    
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void reconcileWalletAndPayments() {
        // 1. Find all wallet transactions WHERE:
        //    - status = 'RESERVED'
        //    - createdAt < (now - 30 minutes) // Give time for normal flow
        
        // 2. For each reserved transaction:
        //    a. Find corresponding payment by walletTransactionId
        //    b. If payment not found → cancel reservation (orphaned)
        //    c. If payment is SUCCESS → confirm reservation
        //    d. If payment is FAILED/EXPIRED → cancel reservation
        //    e. If payment is PENDING → check provider status, then reconcile
        
        // 3. Log all reconciliation actions for audit
    }
    
    @Scheduled(fixedRate = 1800000) // Run every 30 minutes
    public void checkStuckPendingPayments() {
        // 1. Find all payments WHERE:
        //    - status = 'PENDING'
        //    - updatedAt < (now - 1 hour)
        
        // 2. For each stuck payment:
        //    a. Query payment provider for current status
        //    b. Update payment status accordingly
        //    c. Confirm or cancel wallet reservation based on result
        
        // 3. Log all status updates for monitoring
    }
}
```

**Repository methods needed:**

```java
// PaymentRepository
@Query("SELECT p FROM Payments p WHERE p.status = 'PENDING' AND p.updatedAt < :cutoffTime")
List<Payments> findStuckPendingPayments(@Param("cutoffTime") Instant cutoffTime);

// WalletTransactionRepository
@Query("SELECT wt FROM WalletTransactions wt WHERE wt.status = 'RESERVED' AND wt.createdAt < :cutoffTime")
List<WalletTransactions> findStaleReservations(@Param("cutoffTime") Instant cutoffTime);
```

---

## 3. Payment Provider Webhook Handler (Alternative to Polling)

**Purpose:** Receive real-time status updates from payment providers instead of polling.

**Implementation Details:**

```java
@RestController
@RequestMapping("/webhooks")
public class PaymentWebhookController {
    
    @PostMapping("/provider/{providerName}")
    public ResponseEntity<Void> handleProviderWebhook(
            @PathVariable String providerName,
            @RequestBody String payload,
            @RequestHeader("X-Signature") String signature) {
        
        // 1. Verify webhook signature (provider-specific)
        // 2. Parse payload and extract transaction status
        // 3. Find payment by providerTransactionId
        // 4. Update payment status
        // 5. Confirm or cancel wallet reservation
        // 6. Return 200 OK to acknowledge receipt
        
        return ResponseEntity.ok().build();
    }
}
```

---

## Priority Order

1. **Payment Expiration Job** - Low complexity, high value (prevents database bloat)
2. **Wallet-Payment Reconciliation Job** - Medium complexity, critical for data consistency
3. **Payment Provider Webhooks** - Provider-dependent, improves UX for PENDING payments

---

## Notes

- All scheduled jobs should be idempotent (safe to run multiple times)
- Use distributed locking (e.g., ShedLock) if running multiple service instances
- Consider using Spring Batch for large-scale reconciliation
- All reconciliation actions should be logged for audit purposes
- Set up monitoring/alerting for reconciliation failures

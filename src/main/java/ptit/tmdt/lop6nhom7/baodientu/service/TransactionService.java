package ptit.tmdt.lop6nhom7.baodientu.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.tmdt.lop6nhom7.baodientu.dto.TransactionResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.VNPayIpnResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.Transaction;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.entity.VipPackage;
import ptit.tmdt.lop6nhom7.baodientu.enums.TransactionStatus;
import ptit.tmdt.lop6nhom7.baodientu.repository.TransactionRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.VipPackageRepo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Transaction service - handles payment flow and VIP subscription logic.
 * 
 * Payment Flow:
 * 1. Initiate Payment: Create pending transaction, generate VNPay URL
 * 2. VNPay Return: User redirected back from VNPay (informational only)
 * 3. VNPay IPN: VNPay server notifies us of payment result (authoritative)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepo transactionRepository;
    private final VipPackageRepo vipPackageRepository;
    private final UserRepo userRepository;
    private final VNPayService vnPayService;

    /**
     * Step 1: Initiate payment - Create transaction and generate VNPay URL.
     * 
     * Only called once per user purchase flow.
     * Returns VNPay payment URL for frontend to redirect to.
     */
    @Transactional
    public String initiatePayment(Integer packageId, HttpServletRequest httpRequest) {
        log.info("Initiating payment for packageId={}", packageId);
        
        User user = getCurrentUser();
        VipPackage vipPackage = vipPackageRepository.findById(packageId)
                .orElseThrow(() -> {
                    log.error("Package not found: {}", packageId);
                    return new RuntimeException("VIP package not found");
                });
        
        // Create transaction in PENDING state
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setPackageField(vipPackage);
        transaction.setAmount(vipPackage.getPrice());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(Instant.now());
        transaction = transactionRepository.save(transaction);
        
        log.info("Transaction created: id={}, user={}, amount={}", 
                transaction.getId(), user.getId(), transaction.getAmount());
        
        // Use transaction ID as payment code (unique identifier for VNPay)
        String paymentCode = String.valueOf(transaction.getId());
        transaction.setPaymentCode(paymentCode);
        transaction = transactionRepository.save(transaction);
        
        // Generate VNPay payment URL
        VNPayService.PaymentRequest paymentRequest = new VNPayService.PaymentRequest(
                paymentCode,
                vipPackage.getPrice(),
                vipPackage.getName()
        );
        
        String paymentUrl = vnPayService.generatePaymentUrl(paymentRequest, httpRequest);
        log.info("Payment URL generated for txnRef={}", paymentCode);
        
        return paymentUrl;
    }

    /**
     * Step 2: VNPay Return Callback (informational only).
     * 
     * Called when user is redirected back from VNPay payment gateway.
     * This endpoint should ONLY display result to user.
     * DO NOT update database here - all updates must be done in IPN handler.
     * 
     * VNPay will retry IPN if it doesn't get proper response,
     * so return response immediately to prevent duplicate updates.
     */
    public boolean getPaymentResult(Map<String, String> params) {
        log.debug("Processing VNPay return callback");
        
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        
        // Verify signature
        if (!vnPayService.verifyCallback(params)) {
            log.warn("Invalid signature in return callback for txnRef={}", txnRef);
            return false;
        }
        
        // Check response code
        boolean success = "00".equals(responseCode);
        log.info("Payment result for txnRef={}: {}", txnRef, success ? "SUCCESS" : "FAILED");
        
        return success;
    }

    /**
     * Step 3: VNPay IPN Callback (authoritative payment result).
     * 
     * This is the ONLY endpoint that should update transaction status and activate VIP.
     * VNPay will retry automatically if response code is not "00".
     * 
     * Validation order (per VNPay spec):
     * 1. Verify signature
     * 2. Check if transaction exists
     * 3. Verify amount
     * 4. Check if already processed (prevent duplicate activation)
     * 5. Update status and activate VIP if payment succeeded
     */
    @Transactional
    public VNPayIpnResponse handleIpn(Map<String, String> params) {
        log.info("Processing VNPay IPN callback");
        
        // ① Verify signature
        if (!vnPayService.verifyCallback(params)) {
            log.error("Invalid signature in IPN");
            return VNPayIpnResponse.invalidSignature();
        }
        
        String txnRef = params.get("vnp_TxnRef");
        log.info("IPN received for txnRef={}", txnRef);
        
        // ② Find transaction
        Optional<Transaction> transactionOpt = transactionRepository.findByPaymentCode(txnRef);
        if (transactionOpt.isEmpty()) {
            log.error("Transaction not found for txnRef={}", txnRef);
            return VNPayIpnResponse.orderNotFound();
        }
        
        Transaction transaction = transactionOpt.get();
        
        // ③ Verify amount
        long expectedAmount = transaction.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();
        long receivedAmount = Long.parseLong(params.getOrDefault("vnp_Amount", "0"));
        
        if (expectedAmount != receivedAmount) {
            log.error("Amount mismatch for txnRef={}: expected={}, received={}", 
                    txnRef, expectedAmount, receivedAmount);
            return VNPayIpnResponse.invalidAmount();
        }
        
        // ④ Check if already processed (prevent duplicate activation)
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.info("Transaction already processed: txnRef={}, status={}", 
                    txnRef, transaction.getStatus());
            return VNPayIpnResponse.alreadyConfirmed();
        }
        
        // ⑤ Update status based on response code and activate VIP
        String responseCode = params.get("vnp_ResponseCode");
        
        if ("00".equals(responseCode)) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            activateUserVip(transaction.getUser(), transaction.getPackageField().getDurationDays());
            log.info("Payment successful and VIP activated for txnRef={}, userId={}", 
                    txnRef, transaction.getUser().getId());
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            log.info("Payment failed for txnRef={}, responseCode={}", txnRef, responseCode);
        }
        
        transactionRepository.save(transaction);
        return VNPayIpnResponse.success();
    }

    /**
     * Get transaction history for current user.
     * Only shows transactions, doesn't verify VIP status.
     */
    public List<TransactionResponse> getMyTransactions() {
        User user = getCurrentUser();
        log.debug("Fetching transactions for userId={}", user.getId());
        
        return transactionRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Activate VIP subscription for user.
     * Extends VIP expiry if already active, otherwise sets new expiry.
     */
    private void activateUserVip(User user, int durationDays) {
        Instant now = Instant.now();
        Instant newExpiryDate;
        
        // If VIP is already active, extend from current expiry
        if (user.getVipExpiryDate() != null && user.getVipExpiryDate().isAfter(now)) {
            newExpiryDate = user.getVipExpiryDate().plus(durationDays, ChronoUnit.DAYS);
            log.info("Extending VIP for userId={} from {} to {}", 
                    user.getId(), user.getVipExpiryDate(), newExpiryDate);
        } else {
            // Otherwise, start new VIP from now
            newExpiryDate = now.plus(durationDays, ChronoUnit.DAYS);
            log.info("Starting new VIP for userId={} until {}", user.getId(), newExpiryDate);
        }
        
        user.setVipExpiryDate(newExpiryDate);
        userRepository.save(user);
    }

    /**
     * Get currently authenticated user from security context.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not found");
        }

        Object principal = authentication.getPrincipal();
        Long userId;
        if (principal instanceof Number number) {
            userId = number.longValue();
        } else {
            userId = Long.parseLong(authentication.getName());
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Convert transaction entity to response DTO.
     */
    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .packageName(transaction.getPackageField().getName())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .paymentCode(transaction.getPaymentCode())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}

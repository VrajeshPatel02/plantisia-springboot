package com.nikyfarm.service;

import com.nikyfarm.dto.CreatePaymentRequestDto;
import com.nikyfarm.dto.PaymentOrderResponseDto;
import com.nikyfarm.dto.VerifyPaymentRequestDto;
import com.nikyfarm.entity.Order;
import com.nikyfarm.entity.OrderItems;
import com.nikyfarm.entity.Payments;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.repository.OrderRepository;
import com.nikyfarm.repository.PaymentsRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentsRepository paymentsRepository;

    private final OrderRepository orderRepository;

    private final RazorpayClient razorpayClient;

    private final CartService cartService;

    private final CouponService couponService;

    private final InventoryService inventoryService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret}")
    private String razorpayWebhookSecret;

    // =========================================================
    // CREATE RAZORPAY ORDER
    // =========================================================

    @Override
    @Transactional
    public PaymentOrderResponseDto createRazorpayOrder(Long userId, CreatePaymentRequestDto request) {

        Order order = orderRepository
                .findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + request.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {

            throw new ResourceNotFoundException("Order not found with ID: " + request.getOrderId());
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {

            throw new BusinessRuleViolationException("Order is already paid");
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {

            throw new BusinessRuleViolationException("Cannot create payment for a cancelled order");
        }

        BigDecimal amount = order.getTotalAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessRuleViolationException("Order amount must be greater than zero");
        }

        long amountInPaise = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        expirePreviousPaymentAttempts(order.getId());

        Payments payment = new Payments();

        payment.setOrder(order);

        payment.setProvider("RAZORPAY");

        payment.setAmount(amount);

        payment.setCurrency("INR");

        payment.setStatus(Payments.Status.CREATED);

        payment = paymentsRepository.save(payment);

        try {

            JSONObject options = new JSONObject();

            options.put("amount", amountInPaise);

            options.put("currency", "INR");

            options.put("receipt", order.getOrderNumber());

            options.put("partial_payment", false);

            JSONObject notes = new JSONObject();

            notes.put("nikyfarm_order_id", order.getId());

            notes.put("nikyfarm_order_number", order.getOrderNumber());

            notes.put("payment_id", payment.getId());

            options.put("notes", notes);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);

            payment.setProviderOrderId(razorpayOrder.get("id"));

            payment.setStatus(Payments.Status.PENDING);

            payment = paymentsRepository.save(payment);

            return PaymentOrderResponseDto.builder()
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .razorpayOrderId(payment.getProviderOrderId())
                    .razorpayKeyId(razorpayKeyId)
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .build();

        } catch (RazorpayException exception) {

            payment.setStatus(Payments.Status.FAILED);

            payment.setFailureCode(exception.getCode());

            payment.setFailureReason(exception.getDescription());

            paymentsRepository.save(payment);

            throw new BusinessRuleViolationException(
                    "Unable to create Razorpay payment order: " + exception.getDescription());
        }
    }

    // =========================================================
    // VERIFY RAZORPAY PAYMENT
    // =========================================================

    @Override
    @Transactional
    public void verifyRazorpayPayment(Long userId, VerifyPaymentRequestDto request) {

        Payments payment = paymentsRepository
                .findByProviderOrderIdForUpdate(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Order order = payment.getOrder();

        if (!order.getUser().getId().equals(userId)) {

            throw new ResourceNotFoundException("Payment not found");
        }

        if (!payment.getProviderOrderId().equals(request.getRazorpayOrderId())) {

            throw new BusinessRuleViolationException("Razorpay order ID mismatch");
        }

        if (payment.getStatus() == Payments.Status.SUCCESS) {

            if (payment.getProviderPaymentId() != null
                    && !payment.getProviderPaymentId().equals(request.getRazorpayPaymentId())) {

                throw new BusinessRuleViolationException("Payment has already been completed");
            }

            return;
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {

            throw new BusinessRuleViolationException("Order is already paid");
        }

        JSONObject attributes = new JSONObject();

        attributes.put("razorpay_order_id", request.getRazorpayOrderId());

        attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());

        attributes.put("razorpay_signature", request.getRazorpaySignature());

        try {

            boolean signatureValid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);

            if (!signatureValid) {

                markPaymentAsFailed(payment, "INVALID_SIGNATURE", "Razorpay payment signature verification failed");

                throw new BusinessRuleViolationException("Payment verification failed");
            }

        } catch (RazorpayException exception) {

            markPaymentAsFailed(payment, "SIGNATURE_VERIFICATION_ERROR", exception.getDescription());

            throw new BusinessRuleViolationException("Unable to verify payment");
        }

        confirmPaymentSuccess(payment, request.getRazorpayPaymentId());
    }

    // =========================================================
    // RAZORPAY WEBHOOK
    // =========================================================

    @Override
    @Transactional
    public void processRazorpayWebhook(String payload, String webhookSignature) {

        if (webhookSignature == null || webhookSignature.isBlank()) {

            throw new BusinessRuleViolationException("Missing Razorpay webhook signature");
        }

        boolean signatureValid = verifyWebhookSignature(payload, webhookSignature);

        if (!signatureValid) {

            throw new BusinessRuleViolationException("Invalid Razorpay webhook signature");
        }

        JSONObject webhookPayload = new JSONObject(payload);

        String event = webhookPayload.optString("event");

        if (event == null || event.isBlank()) {

            throw new BusinessRuleViolationException("Webhook event is missing");
        }

        switch (event) {
            case "payment.captured" -> handlePaymentCaptured(webhookPayload);

            case "payment.failed" -> handlePaymentFailed(webhookPayload);

            default -> {
                // Ignore unsupported events.
            }
        }
    }

    @Override
    @Transactional
    public void refundOrderPayment(Long orderId) {

        List<Payments> payments = paymentsRepository.findProviderPaymentsByOrderIdForUpdate(orderId);

        if (payments.isEmpty()) {

            throw new ResourceNotFoundException("No provider payment found for order: " + orderId);
        }

        Payments payment = null;

        for (Payments currentPayment : payments) {

            // Refund already completed.
            // Safe to return because this method is idempotent.
            if (currentPayment.getStatus() == Payments.Status.REFUNDED) {

                return;
            }

            // Find the latest refundable payment.
            if (currentPayment.getStatus() == Payments.Status.CAPTURED
                    || currentPayment.getStatus() == Payments.Status.SUCCESS) {

                payment = currentPayment;
                break;
            }
        }

        if (payment == null) {

            throw new BusinessRuleViolationException("No refundable payment found for order: " + orderId);
        }

        if (payment.getProviderPaymentId() == null
                || payment.getProviderPaymentId().isBlank()) {

            throw new BusinessRuleViolationException("Provider payment ID is missing");
        }

        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessRuleViolationException("Payment amount is invalid");
        }

        try {

            // Create full refund in Razorpay.
            com.razorpay.Refund razorpayRefund = razorpayClient.payments.refund(payment.getProviderPaymentId());

            String refundId = razorpayRefund.get("id");

            // Update local payment record.
            payment.setProviderRefundId(refundId);

            payment.setRefundAmount(payment.getAmount());

            payment.setRefundedAt(LocalDateTime.now());

            payment.setStatus(Payments.Status.REFUNDED);

            payment.setFailureCode(null);

            payment.setFailureReason(null);

            paymentsRepository.save(payment);

            // Update order payment status.
            Order order = payment.getOrder();

            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);

            orderRepository.save(order);

        } catch (RazorpayException exception) {

            throw new BusinessRuleViolationException("Unable to refund payment: " + exception.getDescription());
        }
    }

    // =========================================================
    // PAYMENT CAPTURED WEBHOOK
    // =========================================================

    private void handlePaymentCaptured(JSONObject webhookPayload) {

        JSONObject paymentEntity =
                webhookPayload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");

        String razorpayPaymentId = paymentEntity.getString("id");

        String razorpayOrderId = paymentEntity.getString("order_id");

        Payments payment = paymentsRepository
                .findByProviderOrderIdForUpdate(razorpayOrderId)
                .orElse(null);

        if (payment == null) {

            return;
        }

        confirmPaymentSuccess(payment, razorpayPaymentId);
    }

    // =========================================================
    // PAYMENT FAILED WEBHOOK
    // =========================================================

    private void handlePaymentFailed(JSONObject webhookPayload) {

        JSONObject paymentEntity =
                webhookPayload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");

        String razorpayOrderId = paymentEntity.optString("order_id");

        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {

            return;
        }

        Payments payment = paymentsRepository
                .findByProviderOrderIdForUpdate(razorpayOrderId)
                .orElse(null);

        if (payment == null) {

            return;
        }

        if (payment.getStatus() == Payments.Status.SUCCESS || payment.getStatus() == Payments.Status.CAPTURED) {

            return;
        }

        String errorCode = paymentEntity.optString("error_code", "PAYMENT_FAILED");

        String errorDescription = paymentEntity.optString("error_description", "Payment failed at Razorpay");

        markPaymentAsFailed(payment, errorCode, errorDescription);
    }

    // =========================================================
    // PAYMENT SUCCESS
    // =========================================================

    private void confirmPaymentSuccess(Payments payment, String razorpayPaymentId) {

        Order order = orderRepository
                .findByIdForPaymentUpdate(payment.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // -----------------------------------------------------
        // ALREADY PAID
        // -----------------------------------------------------

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {

            reconcileAlreadyPaidOrder(payment, razorpayPaymentId);

            return;
        }

        // -----------------------------------------------------
        // PAYMENT ID CONSISTENCY
        // -----------------------------------------------------

        if (payment.getProviderPaymentId() != null
                && !payment.getProviderPaymentId().equals(razorpayPaymentId)) {

            throw new BusinessRuleViolationException("Payment ID mismatch");
        }

        // -----------------------------------------------------
        // CANCELLED ORDER
        // -----------------------------------------------------

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {

            markPaymentAsCaptured(
                    payment,
                    razorpayPaymentId,
                    "ORDER_CANCELLED",
                    "Payment was captured for a cancelled order. Refund or reconciliation is required.");

            return;
        }

        // -----------------------------------------------------
        // MARK EXTERNAL PAYMENT AS CAPTURED
        // -----------------------------------------------------

        markPaymentCaptured(payment, razorpayPaymentId);

        // -----------------------------------------------------
        // ATOMIC INVENTORY DEDUCTION
        // -----------------------------------------------------

        List<OrderItems> deductedItems = new ArrayList<>();

        try {

            deductOrderInventory(order, deductedItems);

        } catch (Exception exception) {

            restoreDeductedInventory(deductedItems);

            markPaymentAsCaptured(payment, razorpayPaymentId, "INVENTORY_DEDUCTION_FAILED", exception.getMessage());

            return;
        }

        // -----------------------------------------------------
        // MARK PAYMENT SUCCESS
        // -----------------------------------------------------

        payment.setStatus(Payments.Status.SUCCESS);

        payment.setFailureCode(null);

        payment.setFailureReason(null);

        paymentsRepository.save(payment);

        // -----------------------------------------------------
        // MARK ORDER PAID
        // -----------------------------------------------------

        order.setPaymentStatus(Order.PaymentStatus.PAID);

        orderRepository.save(order);

        // -----------------------------------------------------
        // CONSUME COUPON
        // -----------------------------------------------------

        if (order.getCoupon() != null) {

            couponService.consumeCouponUsage(order.getCoupon().getId());
        }

        // -----------------------------------------------------
        // CLEAR CART
        // -----------------------------------------------------

        cartService.clearCart(order.getUser().getId());
    }

    // =========================================================
    // MARK PAYMENT CAPTURED
    // =========================================================

    private void markPaymentCaptured(Payments payment, String razorpayPaymentId) {

        payment.setProviderPaymentId(razorpayPaymentId);

        payment.setStatus(Payments.Status.CAPTURED);

        payment.setFailureCode(null);

        payment.setFailureReason(null);

        if (payment.getPaidAt() == null) {

            payment.setPaidAt(LocalDateTime.now());
        }

        paymentsRepository.save(payment);
    }

    // =========================================================
    // MARK PAYMENT CAPTURED WITH RECONCILIATION ISSUE
    // =========================================================

    private void markPaymentAsCaptured(
            Payments payment, String razorpayPaymentId, String failureCode, String failureReason) {

        payment.setProviderPaymentId(razorpayPaymentId);

        payment.setStatus(Payments.Status.CAPTURED);

        payment.setFailureCode(failureCode);

        payment.setFailureReason(failureReason);

        if (payment.getPaidAt() == null) {

            payment.setPaidAt(LocalDateTime.now());
        }

        paymentsRepository.save(payment);
    }

    // =========================================================
    // INVENTORY DEDUCTION
    // =========================================================

    private void deductOrderInventory(Order order, List<OrderItems> deductedItems) {

        if (order.getItems() == null || order.getItems().isEmpty()) {

            throw new BusinessRuleViolationException("Order contains no items");
        }

        for (OrderItems item : order.getItems()) {

            if (item.getProductVariant() == null) {

                throw new BusinessRuleViolationException("Order item contains an invalid product variant");
            }

            inventoryService.deductStock(item.getProductVariant().getId(), item.getQuantity());

            deductedItems.add(item);
        }
    }

    // =========================================================
    // INVENTORY RESTORATION
    // =========================================================

    private void restoreDeductedInventory(List<OrderItems> deductedItems) {

        for (OrderItems item : deductedItems) {

            try {

                inventoryService.restoreStock(item.getProductVariant().getId(), item.getQuantity());

            } catch (Exception ignored) {

                // Payment remains CAPTURED and reconciliation
                // will be required.
            }
        }
    }

    // =========================================================
    // PAYMENT FAILURE
    // =========================================================

    private void markPaymentAsFailed(Payments payment, String failureCode, String failureReason) {

        if (payment.getStatus() == Payments.Status.SUCCESS || payment.getStatus() == Payments.Status.CAPTURED) {

            return;
        }

        payment.setStatus(Payments.Status.FAILED);

        payment.setFailureCode(failureCode);

        payment.setFailureReason(failureReason);

        paymentsRepository.save(payment);
    }

    // =========================================================
    // WEBHOOK SIGNATURE
    // =========================================================

    private boolean verifyWebhookSignature(String payload, String webhookSignature) {

        try {

            return Utils.verifyWebhookSignature(payload, webhookSignature, razorpayWebhookSecret);

        } catch (RazorpayException exception) {

            throw new BusinessRuleViolationException("Unable to verify Razorpay webhook signature");
        }
    }

    // =========================================================
    // EXPIRE PREVIOUS PAYMENT ATTEMPTS
    // =========================================================

    private void expirePreviousPaymentAttempts(Long orderId) {

        List<Payments> activePayments = paymentsRepository.findByOrderIdAndStatusIn(
                orderId, List.of(Payments.Status.CREATED, Payments.Status.PENDING));

        for (Payments payment : activePayments) {

            payment.setStatus(Payments.Status.FAILED);

            payment.setFailureCode("PAYMENT_REPLACED");

            payment.setFailureReason("Payment attempt replaced by a newer attempt");
        }

        paymentsRepository.saveAll(activePayments);
    }

    // =========================================================
    // PAYMENT RECONCILIATION
    // =========================================================

    private void reconcileAlreadyPaidOrder(Payments payment, String razorpayPaymentId) {

        if (payment.getStatus() == Payments.Status.SUCCESS) {

            if (payment.getProviderPaymentId() != null
                    && !payment.getProviderPaymentId().equals(razorpayPaymentId)) {

                throw new BusinessRuleViolationException("Payment ID mismatch");
            }

            return;
        }

        payment.setProviderPaymentId(razorpayPaymentId);

        payment.setStatus(Payments.Status.FAILED);

        payment.setFailureCode("ORDER_ALREADY_PAID");

        payment.setFailureReason("Another payment attempt already completed this order");

        paymentsRepository.save(payment);
    }
}

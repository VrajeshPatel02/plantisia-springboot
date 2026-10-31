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
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentsRepository paymentsRepository;
    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;
    private final CartService cartService;
    private final CouponService couponService;
    private final InventoryService inventoryService;
    private final TransactionTemplate transactionTemplate;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret}")
    private String razorpayWebhookSecret;

    public PaymentServiceImpl(
            PaymentsRepository paymentsRepository,
            OrderRepository orderRepository,
            RazorpayClient razorpayClient,
            CartService cartService,
            CouponService couponService,
            InventoryService inventoryService,
            PlatformTransactionManager transactionManager) {
        this.paymentsRepository = paymentsRepository;
        this.orderRepository = orderRepository;
        this.razorpayClient = razorpayClient;
        this.cartService = cartService;
        this.couponService = couponService;
        this.inventoryService = inventoryService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private record PaymentInitData(
            Long paymentId,
            Long orderId,
            String orderNumber,
            BigDecimal amount,
            long amountInPaise
    ) {}

    private record RefundPreparationData(
            Long paymentId,
            String providerPaymentId,
            BigDecimal amount,
            boolean alreadyHandled
    ) {}

    // =========================================================
    // CREATE RAZORPAY ORDER
    // =========================================================

    @Override
    public PaymentOrderResponseDto createRazorpayOrder(Long userId, CreatePaymentRequestDto request) {

        // Step 1: Validate and initialize Payment record in a short DB transaction
        PaymentInitData initData = transactionTemplate.execute(status -> {
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

            return new PaymentInitData(payment.getId(), order.getId(), order.getOrderNumber(), amount, amountInPaise);
        });

        if (initData == null) {
            throw new BusinessRuleViolationException("Unable to initialize payment");
        }

        // Step 2: Make external HTTP call to Razorpay (NO open DB transaction or connection)
        com.razorpay.Order razorpayOrder;
        try {
            JSONObject options = new JSONObject();
            options.put("amount", initData.amountInPaise());
            options.put("currency", "INR");
            options.put("receipt", initData.orderNumber());
            options.put("partial_payment", false);

            JSONObject notes = new JSONObject();
            notes.put("nikyfarm_order_id", initData.orderId());
            notes.put("nikyfarm_order_number", initData.orderNumber());
            notes.put("payment_id", initData.paymentId());
            options.put("notes", notes);

            razorpayOrder = razorpayClient.orders.create(options);
        } catch (RazorpayException exception) {
            transactionTemplate.executeWithoutResult(status -> paymentsRepository.findById(initData.paymentId()).ifPresent(p -> {
                p.setStatus(Payments.Status.FAILED);
                p.setFailureCode(exception.getCode());
                p.setFailureReason(exception.getDescription());
                paymentsRepository.save(p);
            }));
            throw new BusinessRuleViolationException(
                    "Unable to create Razorpay payment order: " + exception.getDescription());
        }

        // Step 3: Update Payment record with provider order id in a short DB transaction
        String providerOrderId = razorpayOrder.get("id");
        transactionTemplate.executeWithoutResult(status -> {
            Payments payment = paymentsRepository.findById(initData.paymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
            payment.setProviderOrderId(providerOrderId);
            payment.setStatus(Payments.Status.PENDING);
            paymentsRepository.save(payment);
        });

        return PaymentOrderResponseDto.builder()
                .paymentId(initData.paymentId())
                .orderId(initData.orderId())
                .orderNumber(initData.orderNumber())
                .razorpayOrderId(providerOrderId)
                .razorpayKeyId(razorpayKeyId)
                .amount(initData.amount())
                .currency("INR")
                .build();
    }

    // =========================================================
    // VERIFY RAZORPAY PAYMENT
    // =========================================================

    @Override
    public void verifyRazorpayPayment(Long userId, VerifyPaymentRequestDto request) {

        // Validate signature before modifying database
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", request.getRazorpayOrderId());
        attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
        attributes.put("razorpay_signature", request.getRazorpaySignature());

        try {
            boolean signatureValid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
            if (!signatureValid) {
                transactionTemplate.executeWithoutResult(status -> paymentsRepository.findByProviderOrderIdForUpdate(request.getRazorpayOrderId())
                        .ifPresent(p -> markPaymentAsFailed(p, "INVALID_SIGNATURE", "Razorpay payment signature verification failed")));
                throw new BusinessRuleViolationException("Payment verification failed");
            }
        } catch (RazorpayException exception) {
            transactionTemplate.executeWithoutResult(status -> paymentsRepository.findByProviderOrderIdForUpdate(request.getRazorpayOrderId())
                    .ifPresent(p -> markPaymentAsFailed(p, "SIGNATURE_VERIFICATION_ERROR", exception.getDescription())));
            throw new BusinessRuleViolationException("Unable to verify payment");
        }

        // Execute payment confirmation within transaction boundary
        boolean inventoryFailed = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
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
                return false;
            }

            if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                throw new BusinessRuleViolationException("Order is already paid");
            }

            return confirmPaymentSuccess(payment, request.getRazorpayPaymentId());
        }));

        if (inventoryFailed) {
            throw new BusinessRuleViolationException(
                    "Item is out of stock. Your payment has been automatically refunded.");
        }
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
            case "refund.created" -> handleRefundCreated(webhookPayload);
            case "refund.processed" -> handleRefundProcessed(webhookPayload);
            case "refund.failed" -> handleRefundFailed(webhookPayload);
            default -> {
                // Ignore unsupported events.
            }
        }
    }

    // =========================================================
    // REFUND ORDER PAYMENT
    // =========================================================

    @Override
    public void refundOrderPayment(Long orderId) {

        // Step 1: Validate, lock, and transition state to REFUND_PENDING in a short DB transaction
        RefundPreparationData prepData = transactionTemplate.execute(status -> {
            Order order = orderRepository
                    .findByIdForPaymentUpdate(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Order not found with ID: " + orderId));

            if (order.getPaymentStatus() == Order.PaymentStatus.REFUNDED
                    || order.getPaymentStatus() == Order.PaymentStatus.REFUND_PENDING) {
                return new RefundPreparationData(null, null, null, true);
            }

            if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
                throw new BusinessRuleViolationException(
                        "Only paid orders can be refunded");
            }

            List<Payments> payments = paymentsRepository
                    .findProviderPaymentsByOrderIdForUpdate(orderId);

            if (payments.isEmpty()) {
                throw new ResourceNotFoundException(
                        "No provider payment found for order: " + orderId);
            }

            for (Payments currentPayment : payments) {
                if (currentPayment.getStatus() == Payments.Status.REFUNDED) {
                    order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
                    orderRepository.save(order);
                    return new RefundPreparationData(null, null, null, true);
                }

                if (currentPayment.getStatus() == Payments.Status.REFUND_PENDING) {
                    order.setPaymentStatus(Order.PaymentStatus.REFUND_PENDING);
                    orderRepository.save(order);
                    return new RefundPreparationData(null, null, null, true);
                }
            }

            Payments payment = null;

            for (Payments currentPayment : payments) {
                if (currentPayment.getStatus() == Payments.Status.CAPTURED
                        || currentPayment.getStatus() == Payments.Status.SUCCESS) {
                    payment = currentPayment;
                    break;
                }
            }

            if (payment == null) {
                throw new BusinessRuleViolationException(
                        "No refundable payment found for order: " + orderId);
            }

            if (payment.getProviderPaymentId() == null
                    || payment.getProviderPaymentId().isBlank()) {
                throw new BusinessRuleViolationException(
                        "Provider payment ID is missing");
            }

            if (payment.getAmount() == null
                    || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleViolationException(
                        "Payment amount is invalid");
            }

            payment.setStatus(Payments.Status.REFUND_PENDING);
            paymentsRepository.save(payment);

            order.setPaymentStatus(Order.PaymentStatus.REFUND_PENDING);
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);

            return new RefundPreparationData(payment.getId(), payment.getProviderPaymentId(), payment.getAmount(), false);
        });

        if (prepData == null || prepData.alreadyHandled()) {
            return;
        }

        // Step 2: Make external HTTP call to Razorpay (NO open DB transaction or locks)
        com.razorpay.Refund razorpayRefund;
        try {
            razorpayRefund = razorpayClient.payments.refund(prepData.providerPaymentId());
        } catch (RazorpayException exception) {
            transactionTemplate.executeWithoutResult(status -> {
                Payments p = paymentsRepository.findById(prepData.paymentId()).orElse(null);
                if (p != null && p.getStatus() == Payments.Status.REFUND_PENDING) {
                    p.setStatus(Payments.Status.SUCCESS);
                    p.setFailureCode("REFUND_INITIATION_FAILED");
                    p.setFailureReason(exception.getDescription());
                    paymentsRepository.save(p);
                }
                Order o = orderRepository.findById(orderId).orElse(null);
                if (o != null && o.getPaymentStatus() == Order.PaymentStatus.REFUND_PENDING) {
                    o.setPaymentStatus(Order.PaymentStatus.PAID);
                    orderRepository.save(o);
                }
            });
            throw new BusinessRuleViolationException(
                    "Unable to initiate refund: " + exception.getDescription());
        }

        String refundId = razorpayRefund.get("id");
        if (refundId == null || refundId.isBlank()) {
            throw new BusinessRuleViolationException(
                    "Razorpay refund was created without a refund ID");
        }

        String providerRefundStatus = razorpayRefund.get("status");

        // Step 3: Finalize refund state in a short DB transaction
        transactionTemplate.executeWithoutResult(status -> {
            Payments payment = paymentsRepository.findById(prepData.paymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

            payment.setProviderRefundId(refundId);
            payment.setRefundAmount(prepData.amount());
            payment.setFailureCode(null);
            payment.setFailureReason(null);

            if ("processed".equalsIgnoreCase(providerRefundStatus)) {
                completeRefund(payment);
            } else {
                payment.setStatus(Payments.Status.REFUND_PENDING);
                payment.setRefundedAt(null);
                paymentsRepository.save(payment);

                Order order = orderRepository.findById(orderId).orElse(null);
                if (order != null) {
                    order.setPaymentStatus(Order.PaymentStatus.REFUND_PENDING);
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    orderRepository.save(order);
                }
            }
        });
    }

    private void handleRefundCreated(JSONObject webhookPayload) {

        JSONObject refundEntity = getRefundEntity(webhookPayload);

        String refundId = refundEntity.optString("id");
        String paymentId = refundEntity.optString("payment_id");

        Payments payment = findRefundPaymentForUpdate(refundId, paymentId);

        if (payment == null) {
            return;
        }

        if (payment.getStatus() == Payments.Status.REFUNDED) {
            return;
        }

        if (refundId != null && !refundId.isBlank()) {
            payment.setProviderRefundId(refundId);
        }

        if (payment.getRefundAmount() == null) {
            payment.setRefundAmount(payment.getAmount());
        }

        payment.setStatus(Payments.Status.REFUND_PENDING);
        payment.setRefundedAt(null);
        payment.setFailureCode(null);
        payment.setFailureReason(null);
        paymentsRepository.save(payment);

        Order order = payment.getOrder();

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            order.setPaymentStatus(Order.PaymentStatus.REFUND_PENDING);
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.REFUND_PENDING
                && order.getStatus() != Order.OrderStatus.CANCELLED) {
            order.setStatus(Order.OrderStatus.CANCELLED);
        }
        orderRepository.save(order);
    }

    private void handleRefundProcessed(JSONObject webhookPayload) {

        JSONObject refundEntity = getRefundEntity(webhookPayload);

        String refundId = refundEntity.optString("id");
        String paymentId = refundEntity.optString("payment_id");

        Payments payment = findRefundPaymentForUpdate(refundId, paymentId);

        if (payment == null) {
            return;
        }

        completeRefund(payment);
    }

    private void handleRefundFailed(JSONObject webhookPayload) {

        JSONObject refundEntity = getRefundEntity(webhookPayload);

        String refundId = refundEntity.optString("id");
        String paymentId = refundEntity.optString("payment_id");

        Payments payment = findRefundPaymentForUpdate(refundId, paymentId);

        if (payment == null) {
            return;
        }

        if (payment.getStatus() == Payments.Status.REFUNDED) {
            return;
        }

        payment.setStatus(Payments.Status.SUCCESS);
        payment.setFailureCode(refundEntity.optString("error_code", "REFUND_FAILED"));
        payment.setFailureReason(refundEntity.optString(
                "error_description", "Razorpay refund failed"));
        paymentsRepository.save(payment);

        Order order = payment.getOrder();

        if (order.getPaymentStatus() == Order.PaymentStatus.REFUND_PENDING) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            orderRepository.save(order);
        }
    }

    private JSONObject getRefundEntity(JSONObject webhookPayload) {

        return webhookPayload
                .getJSONObject("payload")
                .getJSONObject("refund")
                .getJSONObject("entity");
    }

    private Payments findRefundPaymentForUpdate(String refundId, String paymentId) {

        if (refundId != null && !refundId.isBlank()) {
            Payments payment = paymentsRepository
                    .findByProviderRefundIdForUpdate(refundId)
                    .orElse(null);

            if (payment != null) {
                return payment;
            }
        }

        if (paymentId != null && !paymentId.isBlank()) {
            return paymentsRepository
                    .findByProviderPaymentIdForUpdate(paymentId)
                    .orElse(null);
        }

        return null;
    }

    private void completeRefund(Payments payment) {

        if (payment.getStatus() == Payments.Status.REFUNDED) {
            return;
        }

        payment.setStatus(Payments.Status.REFUNDED);
        payment.setRefundAmount(
                payment.getRefundAmount() != null
                        ? payment.getRefundAmount()
                        : payment.getAmount());
        payment.setRefundedAt(
                payment.getRefundedAt() != null
                        ? payment.getRefundedAt()
                        : LocalDateTime.now());
        payment.setFailureCode(null);
        payment.setFailureReason(null);
        paymentsRepository.save(payment);

        Order order = orderRepository
                .findByIdForPaymentUpdate(payment.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + payment.getOrder().getId()));

        if (order.getPaymentStatus() != Order.PaymentStatus.REFUNDED) {
            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            orderRepository.save(order);
        }

        // Only restore inventory if stock was actually deducted for this order.
        // If refund was triggered because inventory deduction failed, stock was never deducted.
        if (!"INVENTORY_DEDUCTION_FAILED".equals(payment.getFailureCode())) {
            restoreOrderInventory(order);
        }

        if (order.getStatus() != Order.OrderStatus.CANCELLED) {
            order.setStatus(Order.OrderStatus.CANCELLED);
        }
        orderRepository.save(order);
    }

    private void restoreOrderInventory(Order order) {

        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        for (OrderItems item : order.getItems()) {
            if (item.getProductVariant() == null) {
                continue;
            }

            inventoryService.restoreStock(
                    item.getProductVariant().getId(),
                    item.getQuantity());
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

    /**
     * Confirms a captured payment, executes atomic inventory deduction, and transitions order state.
     *
     * @return true if inventory deduction failed and auto-refund was dispatched, false otherwise
     */
    private boolean confirmPaymentSuccess(Payments payment, String razorpayPaymentId) {

        Order order = orderRepository
                .findByIdForPaymentUpdate(payment.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // -----------------------------------------------------
        // ALREADY PAID
        // -----------------------------------------------------

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            reconcileAlreadyPaidOrder(payment, razorpayPaymentId);
            return false;
        }

        // -----------------------------------------------------
        // PAYMENT ID CONSISTENCY
        // -----------------------------------------------------

        if (payment.getProviderPaymentId() != null
                && !payment.getProviderPaymentId().equals(razorpayPaymentId)) {
            throw new BusinessRuleViolationException("Payment ID mismatch");
        }

        // -----------------------------------------------------
        // CANCELLED ORDER (Auto-refund to avoid stranded funds)
        // -----------------------------------------------------

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            handleCancelledOrderPaymentWithRefund(order, payment, razorpayPaymentId);
            return true;
        }

        // -----------------------------------------------------
        // MARK EXTERNAL PAYMENT AS CAPTURED
        // -----------------------------------------------------

        markPaymentCaptured(payment, razorpayPaymentId);

        // -----------------------------------------------------
        // ATOMIC INVENTORY DEDUCTION (With Immediate Auto-Refund)
        // -----------------------------------------------------

        List<OrderItems> deductedItems = new ArrayList<>();

        try {
            deductOrderInventory(order, deductedItems);
        } catch (Exception exception) {
            restoreDeductedInventory(deductedItems);
            handleInventoryDeductionFailureWithRefund(order, payment, razorpayPaymentId, exception.getMessage());
            return true;
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
        // CONSUME COUPON (Non-critical to avoid rolling back paid order)
        // -----------------------------------------------------

        if (order.getCoupon() != null) {
            try {
                couponService.consumeCouponUsage(order.getCoupon().getId());
            } catch (Exception e) {
                log.warn("Failed to consume coupon {} for paid order {}: {}", order.getCoupon().getId(), order.getId(), e.getMessage());
            }
        }

        // -----------------------------------------------------
        // CLEAR CART (Non-critical to avoid rolling back paid order)
        // -----------------------------------------------------

        try {
            cartService.clearCart(order.getUser().getId());
        } catch (Exception e) {
            log.warn("Failed to clear cart for user {}: {}", order.getUser().getId(), e.getMessage());
        }

        return false;
    }

    private void handleInventoryDeductionFailureWithRefund(
            Order order, Payments payment, String razorpayPaymentId, String failureDetail) {

        payment.setProviderPaymentId(razorpayPaymentId);
        payment.setFailureCode("INVENTORY_DEDUCTION_FAILED");
        payment.setFailureReason("Stock deduction failed: " + failureDetail);
        if (payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);

        try {
            com.razorpay.Refund refund = razorpayClient.payments.refund(razorpayPaymentId);
            String refundId = refund.get("id");
            String refundStatus = refund.get("status");

            payment.setProviderRefundId(refundId);
            payment.setRefundAmount(payment.getAmount());

            if ("processed".equalsIgnoreCase(refundStatus)) {
                payment.setStatus(Payments.Status.REFUNDED);
                payment.setRefundedAt(LocalDateTime.now());
                order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(Payments.Status.REFUND_PENDING);
                order.setPaymentStatus(Order.PaymentStatus.REFUND_PENDING);
            }
        } catch (RazorpayException refundEx) {
            log.error("Failed to auto-refund payment {} after inventory exhaustion: {}", razorpayPaymentId, refundEx.getDescription(), refundEx);
            payment.setStatus(Payments.Status.CAPTURED);
            payment.setFailureCode("INVENTORY_DEDUCTION_FAILED_AUTO_REFUND_ERROR");
            payment.setFailureReason("Stock deduction failed (" + failureDetail + "), auto-refund error: " + refundEx.getDescription());
            order.setPaymentStatus(Order.PaymentStatus.REFUND_PENDING);
        }

        orderRepository.save(order);
        paymentsRepository.save(payment);
    }

    private void handleCancelledOrderPaymentWithRefund(
            Order order, Payments payment, String razorpayPaymentId) {

        payment.setProviderPaymentId(razorpayPaymentId);
        if (payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }

        try {
            com.razorpay.Refund refund = razorpayClient.payments.refund(razorpayPaymentId);
            String refundId = refund.get("id");
            String refundStatus = refund.get("status");

            payment.setProviderRefundId(refundId);
            payment.setRefundAmount(payment.getAmount());

            if ("processed".equalsIgnoreCase(refundStatus)) {
                payment.setStatus(Payments.Status.REFUNDED);
                payment.setRefundedAt(LocalDateTime.now());
                order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(Payments.Status.REFUND_PENDING);
                order.setPaymentStatus(Order.PaymentStatus.REFUND_PENDING);
            }
        } catch (RazorpayException refundEx) {
            log.error("Failed to auto-refund captured payment {} for cancelled order {}: {}", razorpayPaymentId, order.getId(), refundEx.getDescription(), refundEx);
            payment.setStatus(Payments.Status.CAPTURED);
            payment.setFailureCode("ORDER_CANCELLED_AUTO_REFUND_ERROR");
            payment.setFailureReason("Payment captured for cancelled order; auto-refund error: " + refundEx.getDescription());
            order.setPaymentStatus(Order.PaymentStatus.REFUND_PENDING);
        }

        orderRepository.save(order);
        paymentsRepository.save(payment);
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
            } catch (Exception e) {
                log.warn("Failed to restore stock for variant {}: {}", item.getProductVariant().getId(), e.getMessage());
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

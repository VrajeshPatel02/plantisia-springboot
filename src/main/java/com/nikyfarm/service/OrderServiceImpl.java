package com.nikyfarm.service;

import com.nikyfarm.dto.*;
import com.nikyfarm.entity.*;
import com.nikyfarm.entity.Order.OrderStatus;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.OrderMapper;
import com.nikyfarm.repository.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;

    private final AddressesRepository addressesRepository;

    private final ProductVariantRepository productVariantRepository;

    private final CartRepository cartRepository;

    private final CouponService couponService;

    private final InventoryService inventoryService;

    private final PaymentService paymentService;

    // =========================================================
    // CREATE ORDER FROM CART
    // =========================================================

    @Override
    @Transactional
    public OrderResponseDto createOrderFromCart(
            Long userId, Long shippingAddressId, Long billingAddressId, String couponCode) {

        Cart cart = cartRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart empty or not found for user ID: " + userId));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {

            throw new BusinessRuleViolationException("Cannot place order with an empty cart");
        }

        List<OrderItemRequestDto> items = new ArrayList<>();

        for (CartItem item : cart.getItems()) {

            if (item.getProductVariant() == null) {

                throw new BusinessRuleViolationException("Cart contains an invalid product variant");
            }

            OrderItemRequestDto dto = new OrderItemRequestDto();

            dto.setProductVariantId(item.getProductVariant().getId());

            dto.setQuantity(item.getQuantity());

            items.add(dto);
        }

        OrderCreateRequestDto createRequest = new OrderCreateRequestDto();

        createRequest.setShippingAddressId(shippingAddressId);

        createRequest.setBillingAddressId(billingAddressId);

        createRequest.setCouponCode(couponCode);

        createRequest.setItems(items);

        /*
         * IMPORTANT:
         *
         * Cart is NOT cleared here.
         *
         * Cart remains available until
         * verified payment success.
         */
        return createDirectOrder(userId, createRequest);
    }

    // =========================================================
    // CREATE DIRECT ORDER
    // =========================================================

    @Override
    @Transactional
    public OrderResponseDto createDirectOrder(Long userId, OrderCreateRequestDto request) {

        validateOrderRequest(request);

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // -----------------------------------------------------
        // ADDRESS VALIDATION
        // -----------------------------------------------------

        Addresses shippingAddress = findUserAddressOrThrow(request.getShippingAddressId(), userId, "Shipping address");

        Addresses billingAddress = request.getBillingAddressId() != null
                ? findUserAddressOrThrow(request.getBillingAddressId(), userId, "Billing address")
                : shippingAddress;

        // -----------------------------------------------------
        // DUPLICATE VARIANT VALIDATION
        // -----------------------------------------------------

        validateDuplicateVariants(request.getItems());

        // -----------------------------------------------------
        // CREATE ORDER
        // -----------------------------------------------------

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .status(OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.UNPAID)
                .shippingFee(BigDecimal.ZERO)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        // -----------------------------------------------------
        // PROCESS ORDER ITEMS
        // -----------------------------------------------------

        for (OrderItemRequestDto itemDto : request.getItems()) {

            validateItemQuantity(itemDto.getQuantity());

            ProductVariant variant = productVariantRepository
                    .findById(itemDto.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product variant not found: " + itemDto.getProductVariantId()));

            // ---------------------------------------------
            // PRODUCT / VARIANT VALIDATION
            // ---------------------------------------------

            validateVariantForOrder(variant);

            // ---------------------------------------------
            // STOCK AVAILABILITY VALIDATION
            //
            // IMPORTANT:
            //
            // This does NOT reserve or deduct stock.
            //
            // Final atomic deduction happens only
            // after verified payment success.
            // ---------------------------------------------

            validateStockAvailability(variant, itemDto.getQuantity());

            // ---------------------------------------------
            // LIVE PRICE SNAPSHOT
            // ---------------------------------------------

            BigDecimal unitPrice = getSellingPrice(variant);

            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));

            // ---------------------------------------------
            // ORDER ITEM SNAPSHOT
            // ---------------------------------------------

            OrderItems orderItem = OrderItems.builder()
                    .product(variant.getProduct())
                    .productVariant(variant)
                    .productNameSnapshot(variant.getProduct().getName())
                    .variantLabelSnapshot(variant.getVariantLabel())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(itemTotal)
                    .build();

            order.addItem(orderItem);

            subtotal = subtotal.add(itemTotal);
        }

        // -----------------------------------------------------
        // ORDER SUBTOTAL
        // -----------------------------------------------------

        order.setSubtotal(subtotal);

        // -----------------------------------------------------
        // COUPON VALIDATION
        // -----------------------------------------------------

        BigDecimal discount = BigDecimal.ZERO;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {

            Coupon coupon = couponService.validateCouponForOrder(request.getCouponCode(), subtotal);

            discount = coupon.calculateDiscount(subtotal);

            order.setCoupon(coupon);
        }

        // -----------------------------------------------------
        // FINAL TOTAL
        // -----------------------------------------------------

        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;

        BigDecimal totalAmount = subtotal.subtract(discount).add(shippingFee);

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException("Order total cannot be negative");
        }

        order.setDiscount(discount);

        order.setTotalAmount(totalAmount);

        // -----------------------------------------------------
        // SAVE PENDING ORDER
        // -----------------------------------------------------

        Order savedOrder = orderRepository.save(order);

        return OrderMapper.toResponseDto(savedOrder);
    }

    // =========================================================
    // GET ORDER BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id, Long currentUserId, boolean isAdmin) {

        Order order = orderRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        if (!isAdmin && !order.getUser().getId().equals(currentUserId)) {

            throw new ResourceNotFoundException("Order not found with ID: " + id);
        }

        return OrderMapper.toResponseDto(order);
    }

    // =========================================================
    // GET ORDER BY ORDER NUMBER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderByOrderNumber(String orderNumber, Long currentUserId, boolean isAdmin) {

        Order order = orderRepository
                .findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        if (!isAdmin && !order.getUser().getId().equals(currentUserId)) {

            throw new ResourceNotFoundException("Order not found: " + orderNumber);
        }

        return OrderMapper.toResponseDto(order);
    }

    // =========================================================
    // GET USER ORDERS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getUserOrders(Long userId, Pageable pageable) {

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(OrderMapper::toResponseDto);
    }

    // =========================================================
    // GET ALL ORDERS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getAllOrders(OrderStatus status, Pageable pageable) {

        if (status != null) {

            return orderRepository
                    .findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(OrderMapper::toResponseDto);
        }

        return orderRepository.findAllByOrderByCreatedAtDesc(pageable).map(OrderMapper::toResponseDto);
    }

    // =========================================================
    // UPDATE ORDER STATUS
    // =========================================================

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(
            Long orderId,
            OrderStatusUpdateDto statusUpdateDto) {

        if (statusUpdateDto == null || statusUpdateDto.getStatus() == null) {
            throw new BusinessRuleViolationException("Order status is required");
        }

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with ID: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus requestedStatus = statusUpdateDto.getStatus();

        if (requestedStatus == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                    "Use the cancel order operation to cancel an order");
        }

        if (currentStatus == requestedStatus) {
            throw new BusinessRuleViolationException(
                    "Order is already in " + requestedStatus + " status");
        }

        /*
         * A customer order cannot enter PROCESSING until
         * payment has been successfully confirmed.
         */
        if (currentStatus == OrderStatus.PENDING
                && requestedStatus == OrderStatus.PROCESSING
                && order.getPaymentStatus() != Order.PaymentStatus.PAID) {

            throw new BusinessRuleViolationException(
                    "Order cannot be processed until payment is confirmed");
        }

        if (!isValidStatusTransition(currentStatus, requestedStatus)) {
            throw new BusinessRuleViolationException(
                    "Invalid order status transition from "
                            + currentStatus
                            + " to "
                            + requestedStatus);
        }

        order.setStatus(requestedStatus);

        Order updatedOrder = orderRepository.save(order);

        return OrderMapper.toResponseDto(updatedOrder);
    }

    // =========================================================
    // CANCEL ORDER
    // =========================================================

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(
            Long orderId,
            Long currentUserId,
            boolean isAdmin) {

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with ID: " + orderId));

        // -----------------------------------------------------
        // OWNERSHIP CHECK
        // -----------------------------------------------------

        if (!isAdmin
                && (currentUserId == null
                || !order.getUser().getId().equals(currentUserId))) {

            throw new ResourceNotFoundException(
                    "Order not found with ID: " + orderId);
        }

        OrderStatus currentStatus = order.getStatus();

        // -----------------------------------------------------
        // ALREADY CANCELLED
        // -----------------------------------------------------

        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                    "Order is already cancelled");
        }

        // -----------------------------------------------------
        // CANNOT CANCEL SHIPPED / DELIVERED
        // -----------------------------------------------------

        if (currentStatus == OrderStatus.SHIPPED
                || currentStatus == OrderStatus.DELIVERED) {

            throw new BusinessRuleViolationException(
                    "Cannot cancel an order that has already been shipped or delivered");
        }

        // -----------------------------------------------------
        // PAID / REFUND PENDING ORDER
        // -----------------------------------------------------

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {

            paymentService.refundOrderPayment(order.getId());

            order = orderRepository
                    .findById(orderId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Order not found with ID: " + orderId));

            if (order.getPaymentStatus() == Order.PaymentStatus.REFUNDED) {
                // PaymentService has already finalized the refund and, for a
                // synchronously processed provider refund, cancelled the order.
                if (order.getStatus() != OrderStatus.CANCELLED) {
                    throw new BusinessRuleViolationException(
                            "Refund completed but order cancellation was not finalized");
                }

                return OrderMapper.toResponseDto(order);
            }

            if (order.getPaymentStatus() == Order.PaymentStatus.REFUND_PENDING) {
                // The order is cancelled once the refund request is accepted.
                // Payment remains REFUND_PENDING until the provider confirms it;
                // inventory is restored only after refund completion.
                if (order.getStatus() != OrderStatus.CANCELLED) {
                    order.setStatus(OrderStatus.CANCELLED);
                    order = orderRepository.save(order);
                }
                return OrderMapper.toResponseDto(order);
            }

            throw new BusinessRuleViolationException(
                    "Order refund could not be confirmed");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.REFUND_PENDING) {
            // Idempotent retry while the provider is processing the refund.
            if (order.getStatus() != OrderStatus.CANCELLED) {
                order.setStatus(OrderStatus.CANCELLED);
                order = orderRepository.save(order);
            }
            return OrderMapper.toResponseDto(order);
        }

        // -----------------------------------------------------
        // UNPAID / FAILED ORDER
        // -----------------------------------------------------

        /*
         * No inventory was deducted for unpaid or failed orders,
         * so there is nothing to restore.
         */
        if (order.getPaymentStatus() == Order.PaymentStatus.UNPAID
                || order.getPaymentStatus() == Order.PaymentStatus.FAILED) {

            order.setStatus(OrderStatus.CANCELLED);

            Order updatedOrder = orderRepository.save(order);

            return OrderMapper.toResponseDto(updatedOrder);
        }

        throw new BusinessRuleViolationException(
                "Order cannot be cancelled in its current payment state");
    }

    // =========================================================
    // CHECKOUT VALIDATION
    // =========================================================

    private void validateOrderRequest(OrderCreateRequestDto request) {

        if (request == null) {

            throw new BusinessRuleViolationException("Order request is required");
        }

        if (request.getShippingAddressId() == null) {

            throw new BusinessRuleViolationException("Shipping address is required");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {

            throw new BusinessRuleViolationException("Order must contain at least one item");
        }
    }

    private void validateDuplicateVariants(List<OrderItemRequestDto> items) {

        Set<Long> variantIds = new HashSet<>();

        for (OrderItemRequestDto item : items) {

            if (item == null || item.getProductVariantId() == null) {

                throw new BusinessRuleViolationException("Product variant ID is required");
            }

            if (!variantIds.add(item.getProductVariantId())) {

                throw new BusinessRuleViolationException(
                        "Duplicate product variant in order: " + item.getProductVariantId());
            }
        }
    }

    private void validateItemQuantity(Integer quantity) {

        if (quantity == null || quantity <= 0) {

            throw new BusinessRuleViolationException("Quantity must be greater than zero");
        }
    }

    // =========================================================
    // PRODUCT / VARIANT VALIDATION
    // =========================================================

    private void validateVariantForOrder(ProductVariant variant) {

        if (variant.getProduct() == null) {

            throw new BusinessRuleViolationException("Product variant is not linked to a product");
        }

        if (!Boolean.TRUE.equals(variant.getProduct().getIsActive())) {

            throw new BusinessRuleViolationException(
                    "Product is not active: " + variant.getProduct().getName());
        }

        if (!Boolean.TRUE.equals(variant.getIsActive())) {

            throw new BusinessRuleViolationException("Product variant is not active: " + variant.getVariantLabel());
        }
    }

    // =========================================================
    // STOCK VALIDATION
    // =========================================================

    private void validateStockAvailability(ProductVariant variant, Integer quantity) {

        Integer stockQuantity = variant.getStockQuantity();

        if (stockQuantity == null || stockQuantity < quantity) {

            throw new BusinessRuleViolationException("Insufficient stock for variant: " + variant.getVariantLabel());
        }
    }

    // =========================================================
    // PRICE
    // =========================================================

    private BigDecimal getSellingPrice(ProductVariant variant) {

        BigDecimal price = variant.getDiscountedPrice() != null ? variant.getDiscountedPrice() : variant.getPrice();

        if (price == null) {

            throw new BusinessRuleViolationException("Product variant price is not available");
        }

        if (price.compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException("Product variant price cannot be negative");
        }

        return price;
    }

    // =========================================================
    // ADDRESS OWNERSHIP
    // =========================================================

    private Addresses findUserAddressOrThrow(Long addressId, Long userId, String addressType) {

        if (addressId == null) {

            throw new BusinessRuleViolationException(addressType + " ID is required");
        }

        return addressesRepository
                .findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(addressType + " not found"));
    }

    // =========================================================
    // INVENTORY RESTORATION
    // =========================================================

    private void restoreOrderInventory(Order order) {

        if (order.getItems() == null || order.getItems().isEmpty()) {

            return;
        }

        for (OrderItems item : order.getItems()) {

            if (item.getProductVariant() == null) {

                continue;
            }

            inventoryService.restoreStock(item.getProductVariant().getId(), item.getQuantity());
        }
    }

    // =========================================================
    // STATUS TRANSITIONS
    // =========================================================

    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus requestedStatus) {

        if (currentStatus == null || requestedStatus == null) {

            return false;
        }

        return switch (currentStatus) {
            case PENDING -> requestedStatus == OrderStatus.PROCESSING;

            case PROCESSING -> requestedStatus == OrderStatus.SHIPPED;

            case SHIPPED -> requestedStatus == OrderStatus.DELIVERED;

            case DELIVERED, CANCELLED -> false;
        };
    }

    // =========================================================
    // ORDER NUMBER
    // =========================================================

    private String generateOrderNumber() {

        return "ORD-"
                + System.currentTimeMillis()
                + "-"
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}

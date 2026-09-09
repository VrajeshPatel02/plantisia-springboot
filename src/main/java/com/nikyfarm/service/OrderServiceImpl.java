package com.nikyfarm.service;

import com.nikyfarm.dto.*;
import com.nikyfarm.entity.*;
import com.nikyfarm.entity.Order.*;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.OrderMapper;
import com.nikyfarm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressesRepository addressesRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public OrderResponseDto createOrderFromCart(Long userId, Long shippingAddressId, Long billingAddressId, String couponCode) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart empty or not found for user ID: " + userId));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessRuleViolationException("Cannot place order with an empty cart");
        }

        List<OrderItemRequestDto> items = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            OrderItemRequestDto dto = new OrderItemRequestDto();
            dto.setProductVariantId(item.getProductVariant().getId());
            dto.setQuantity(item.getQuantity());
            items.add(dto);
        }

        OrderCreateRequestDto createRequest = new OrderCreateRequestDto();
        createRequest.setShippingAddressId(shippingAddressId);
        createRequest.setBillingAddressId(billingAddressId != null ? billingAddressId : shippingAddressId);
        createRequest.setCouponCode(couponCode);
        createRequest.setItems(items);

        OrderResponseDto response = createDirectOrder(userId, createRequest);

        cartService.clearCart(userId);
        return response;
    }

    @Override
    @Transactional
    public OrderResponseDto createDirectOrder(Long userId, OrderCreateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Addresses shippingAddress = addressesRepository.findById(request.getShippingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));

        Addresses billingAddress = request.getBillingAddressId() != null ?
                addressesRepository.findById(request.getBillingAddressId())
                        .orElseThrow(() -> new ResourceNotFoundException("Billing address not found")) : shippingAddress;

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.UNPAID)
                .shippingFee(BigDecimal.ZERO)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequestDto itemDto : request.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + itemDto.getProductVariantId()));

            if (variant.getStockQuantity() < itemDto.getQuantity()) {
                throw new BusinessRuleViolationException("Insufficient stock for variant: " + variant.getVariantLabel());
            }

            variant.setStockQuantity(variant.getStockQuantity() - itemDto.getQuantity());
            productVariantRepository.save(variant);

            BigDecimal unitPrice = BigDecimal.valueOf(variant.getDiscountedPrice() != null ? variant.getDiscountedPrice() : variant.getPrice());
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));

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

        order.setSubtotal(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCode(request.getCouponCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + request.getCouponCode()));

            discount = coupon.calculateDiscount(subtotal);
            order.setCoupon(coupon);
        }

        order.setDiscount(discount);
        order.setTotalAmount(subtotal.subtract(discount).add(order.getShippingFee()));

        Order savedOrder = orderRepository.save(order);
        return OrderMapper.toResponseDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id, Long currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        if (!isAdmin && !order.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Order not found with ID: " + id);
        }

        return OrderMapper.toResponseDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderByOrderNumber(String orderNumber, Long currentUserId, boolean isAdmin) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        if (!isAdmin && !order.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Order not found: " + orderNumber);
        }

        return OrderMapper.toResponseDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(OrderMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getAllOrders(Order.OrderStatus status, Pageable pageable) {
        if (status != null) {
            return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(OrderMapper::toResponseDto);
        }
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(OrderMapper::toResponseDto);
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatusUpdateDto statusUpdateDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        order.setStatus(statusUpdateDto.getStatus());
        if (statusUpdateDto.getPaymentStatus() != null) {
            order.setPaymentStatus(statusUpdateDto.getPaymentStatus());
        }

        Order updatedOrder = orderRepository.save(order);
        return OrderMapper.toResponseDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, Long currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!isAdmin && !order.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId);
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleViolationException("Cannot cancel an order that has already been shipped or delivered");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);

        for (OrderItems item : order.getItems()) {
            if (item.getProductVariant() != null) {
                ProductVariant variant = item.getProductVariant();
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                productVariantRepository.save(variant);
            }
        }

        Order updatedOrder = orderRepository.save(order);
        return OrderMapper.toResponseDto(updatedOrder);
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
package com.nikyfarm.service;

import com.nikyfarm.dto.OrderCreateRequestDto;
import com.nikyfarm.dto.OrderResponseDto;
import com.nikyfarm.dto.OrderStatusUpdateDto;
import com.nikyfarm.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponseDto createOrderFromCart(Long userId, Long shippingAddressId, Long billingAddressId, String couponCode);
    OrderResponseDto createDirectOrder(Long userId, OrderCreateRequestDto request);
    OrderResponseDto getOrderById(Long id, Long currentUserId, boolean isAdmin);
    OrderResponseDto getOrderByOrderNumber(String orderNumber, Long currentUserId, boolean isAdmin);
    Page<OrderResponseDto> getUserOrders(Long userId, Pageable pageable);
    Page<OrderResponseDto> getAllOrders(OrderStatus status, Pageable pageable);
    OrderResponseDto updateOrderStatus(Long orderId, OrderStatusUpdateDto statusUpdateDto);
    OrderResponseDto cancelOrder(Long orderId, Long currentUserId, boolean isAdmin);
}
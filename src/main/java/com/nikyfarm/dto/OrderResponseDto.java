package com.nikyfarm.dto;

import com.nikyfarm.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponseDto {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private Order.OrderStatus status;
    private Order.PaymentStatus paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private AddressResponseDto shippingAddress;
    private AddressResponseDto billingAddress;
    private List<OrderItemResponseDto> items;
    private LocalDateTime createdAt;
}

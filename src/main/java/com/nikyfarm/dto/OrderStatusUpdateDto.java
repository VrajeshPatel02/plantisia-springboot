package com.nikyfarm.dto;

import com.nikyfarm.entity.Order.OrderStatus;
import com.nikyfarm.entity.Order.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateDto {

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    private PaymentStatus paymentStatus;
}
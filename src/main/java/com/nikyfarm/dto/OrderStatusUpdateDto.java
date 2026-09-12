package com.nikyfarm.dto;

import com.nikyfarm.entity.Order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateDto {

    @NotNull(message = "Order status is required")
    private OrderStatus status;
}

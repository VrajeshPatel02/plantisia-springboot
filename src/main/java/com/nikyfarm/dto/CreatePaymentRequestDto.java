package com.nikyfarm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentRequestDto {

    @NotNull(message = "Order ID is required")
    private Long orderId;
}

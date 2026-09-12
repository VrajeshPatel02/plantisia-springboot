package com.nikyfarm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CouponValidationRequestDto {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Order subtotal is required")
    private BigDecimal orderSubtotal;
}

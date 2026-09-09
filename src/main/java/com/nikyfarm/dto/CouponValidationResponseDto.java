package com.nikyfarm.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CouponValidationResponseDto {
    private boolean valid;
    private String code;
    private BigDecimal discountAmount;
    private String message;
}
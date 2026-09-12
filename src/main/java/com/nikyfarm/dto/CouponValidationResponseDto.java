package com.nikyfarm.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CouponValidationResponseDto {
    private boolean valid;
    private String code;
    private BigDecimal discountAmount;
    private String message;
}

package com.nikyfarm.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponseDto {
    private Long id;
    private Long productId;
    private Long productVariantId;
    private String productNameSnapshot;
    private String variantLabelSnapshot;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
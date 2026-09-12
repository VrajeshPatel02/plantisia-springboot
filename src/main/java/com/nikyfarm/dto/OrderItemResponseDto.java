package com.nikyfarm.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

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

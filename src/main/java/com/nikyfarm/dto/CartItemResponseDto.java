package com.nikyfarm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDto {

    private Long id;
    private Long productVariantId;
    private String sku;
    private String variantLabel;
    private Long productId;
    private String productName;
    private String primaryImageUrl;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
    private Integer availableStock;
    /** True when quantity requested exceeds current stockQuantity — lets the client flag it before checkout. */
    private boolean stockInsufficient;
    private LocalDateTime addedAt;
}

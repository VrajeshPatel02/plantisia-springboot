package com.nikyfarm.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {

    private Long id;
    private Long userId;
    private List<CartItemResponseDto> items;

    /** Sum of item quantities (not row count) — what a cart-icon badge should show. */
    private Integer totalItemCount;

    private BigDecimal totalPrice;
}

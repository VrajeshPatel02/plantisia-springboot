package com.nikyfarm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

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

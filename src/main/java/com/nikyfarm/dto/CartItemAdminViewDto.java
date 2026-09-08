package com.nikyfarm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flat, per-row view for the admin cart-items table (`admin/cart-items/list.html`).
 * Deliberately separate from CartItemResponseDto: the admin table lists items across
 * every user's cart (needs userId per row), whereas the customer-facing DTO is always
 * scoped to "my cart" and doesn't need to repeat the owner on every line.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemAdminViewDto {

    private Long id;
    private Long userId;
    private String variantLabel;
    private String sku;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
    private LocalDateTime createdAt;
}

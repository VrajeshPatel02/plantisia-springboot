package com.nikyfarm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line item in a {@link Cart}: a ProductVariant + quantity. Deliberately does NOT snapshot unit
 * price the way {@code OrderItems} does — a cart should always reflect the variant's live
 * price/stock, since nothing has been purchased yet. Price snapshotting belongs at checkout time
 * (OrderItem), not here.
 *
 * <p>The unique constraint on (cart_id, product_variant_id) means "add to cart" for a variant
 * already in the cart is an update (bump quantity), never a duplicate row.
 */
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_cart_item_cart_variant",
                        columnNames = {"cart_id", "product_variant_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public CartItem(Cart cart, ProductVariant productVariant, Integer quantity) {
        this.cart = cart;
        this.productVariant = productVariant;
        this.quantity = quantity;
    }

    @PrePersist
    protected void onCreate() {
        this.addedAt = LocalDateTime.now();
    }
}

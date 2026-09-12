package com.nikyfarm.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The cart aggregate root — exactly one per {@link User}. Replaces the old flat {@code CartItems ->
 * User} shape: line items now hang off a Cart, not off the user directly, which is the standard
 * e-commerce modeling (User 1---1 Cart 1---* CartItem).
 *
 * <p>Kept deliberately "thin": no totals/price columns are persisted here. Totals are derived at
 * read time in {@code CartMapper} from each item's live variant price, so the cart never goes stale
 * if a product's price changes after items were added (unlike an Order/OrderItem, which
 * intentionally snapshots price at purchase time).
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * One cart per user. {@code unique = true} on the FK enforces the 1:1 at the DB level, not just
     * in application code.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Cart(User user) {
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Keeps both sides of the bidirectional association in sync. */
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    /**
     * Keeps both sides of the bidirectional association in sync (needed for orphanRemoval to fire on
     * delete).
     */
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }
}

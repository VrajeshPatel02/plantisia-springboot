package com.nikyfarm.repository;

import com.nikyfarm.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    /**
     * Used by "add to cart" to decide whether to bump an existing line item's quantity or insert a
     * new one.
     */
    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long productVariantId);

    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

    void deleteByCartIdAndProductVariantId(Long cartId, Long productVariantId);

    void deleteByCartId(Long cartId);

    // --- admin listing (AdminCartItemController) ---

    @EntityGraph(attributePaths = {"cart", "cart.user", "productVariant", "productVariant.product"})
    List<CartItem> findAllByOrderByAddedAtDesc();

    @EntityGraph(attributePaths = {"cart", "cart.user", "productVariant", "productVariant.product"})
    List<CartItem> findByCart_User_IdOrderByAddedAtDesc(Long userId);
}

package com.nikyfarm.repository;

import com.nikyfarm.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndProductVariantId(
            Long cartId,
            Long productVariantId
    );

    Optional<CartItem> findByIdAndCartId(
            Long id,
            Long cartId
    );

    void deleteByCartIdAndProductVariantId(
            Long cartId,
            Long productVariantId
    );

    /*
     * Kept because CartItemServiceImpl currently uses this method.
     * Do not remove it unless all existing usages are migrated.
     */
    void deleteByCartId(Long cartId);

    /*
     * Explicit bulk delete used by CartServiceImpl.clearCart().
     * This directly deletes all CartItem rows belonging to the cart.
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    int deleteAllByCartId(@Param("cartId") Long cartId);

    @EntityGraph(
            attributePaths = {
                    "cart",
                    "cart.user",
                    "productVariant",
                    "productVariant.product"
            }
    )
    List<CartItem> findAllByOrderByAddedAtDesc();

    @EntityGraph(
            attributePaths = {
                    "cart",
                    "cart.user",
                    "productVariant",
                    "productVariant.product"
            }
    )
    List<CartItem> findByCart_User_IdOrderByAddedAtDesc(Long userId);
}
package com.nikyfarm.repository;

import com.nikyfarm.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

    void deleteByCartId(Long cartId);

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
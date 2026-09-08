package com.nikyfarm.repository;

import com.nikyfarm.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Fetches a user's cart together with its items and each item's variant/product, in
     * one query — the standard "view cart" read, otherwise lazy-loading each item's
     * variant/product one-by-one is an N+1.
     */
    @EntityGraph(attributePaths = {"items", "items.productVariant", "items.productVariant.product"})
    Optional<Cart> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}

package com.nikyfarm.repository;

import com.nikyfarm.entity.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemsRepository extends JpaRepository<CartItems, Long> {
    List<CartItems> findByUserId(Long userId);
    Optional<CartItems> findByUserIdAndProductVariantId(Long userId, Long productVariantId);
}

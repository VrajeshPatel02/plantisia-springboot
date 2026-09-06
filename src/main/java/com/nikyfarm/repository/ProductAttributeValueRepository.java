package com.nikyfarm.repository;

import com.nikyfarm.entity.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {
    boolean existsByAttributeId(Long attributeId);
    boolean existsByAttributeOptionsId(Long attributeOptionsId);
    boolean existsByProductIdAndAttributeId(Long productId, Long attributeId);
    boolean existsByProductIdAndAttributeIdAndIdNot(Long productId, Long attributeId, Long id);
    List<ProductAttributeValue> findByProductId(Long productId);
}

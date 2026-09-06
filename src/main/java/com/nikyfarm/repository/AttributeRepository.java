package com.nikyfarm.repository;

import com.nikyfarm.entity.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttributeRepository extends JpaRepository<Attribute, Long> {
    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);
    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);
    List<Attribute> findByCategoryId(Long categoryId);
}

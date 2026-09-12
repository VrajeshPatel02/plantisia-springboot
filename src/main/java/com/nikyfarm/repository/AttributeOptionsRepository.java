package com.nikyfarm.repository;

import com.nikyfarm.entity.AttributeOptions;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeOptionsRepository extends JpaRepository<AttributeOptions, Long> {
    boolean existsByAttributeIdAndValueIgnoreCase(Long attributeId, String value);

    boolean existsByAttributeIdAndValueIgnoreCaseAndIdNot(Long attributeId, String value, Long id);

    List<AttributeOptions> findByAttributeId(Long attributeId);
}

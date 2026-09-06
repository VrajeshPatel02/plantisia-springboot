package com.nikyfarm.service;

import com.nikyfarm.dto.ProductAttributeValueRequestDto;
import com.nikyfarm.dto.ProductAttributeValueResponseDto;

import java.util.List;

public interface ProductAttributeValueService {

    ProductAttributeValueResponseDto createValue(Long productId, ProductAttributeValueRequestDto dto);

    ProductAttributeValueResponseDto updateValue(Long productId, Long valueId, ProductAttributeValueRequestDto dto);

    ProductAttributeValueResponseDto getValueById(Long productId, Long valueId);

    List<ProductAttributeValueResponseDto> getValuesByProduct(Long productId);

    void deleteValue(Long productId, Long valueId);
}

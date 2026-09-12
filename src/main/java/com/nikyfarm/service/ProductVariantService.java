package com.nikyfarm.service;

import com.nikyfarm.dto.ProductVariantRequestDto;
import com.nikyfarm.dto.ProductVariantResponseDto;
import java.util.List;

public interface ProductVariantService {

    ProductVariantResponseDto createVariant(Long productId, ProductVariantRequestDto dto);

    ProductVariantResponseDto updateVariant(Long productId, Long variantId, ProductVariantRequestDto dto);

    ProductVariantResponseDto getVariantById(Long productId, Long variantId);

    List<ProductVariantResponseDto> getVariantsByProduct(Long productId);

    void deleteVariant(Long productId, Long variantId);
}

package com.nikyfarm.service;

import com.nikyfarm.dto.ProductRequestDto;
import com.nikyfarm.dto.ProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductService {

    ProductResponseDto createProduct(ProductRequestDto dto);

    ProductResponseDto updateProduct(Long id, ProductRequestDto dto);

    ProductResponseDto getProductById(Long id);

    Page<ProductResponseDto> getProducts(Long categoryId, Pageable pageable);

    Page<ProductResponseDto> getProducts(Long categoryId, String search, Pageable pageable);

    byte[] exportProducts(List<String> fields, String format);

    void deleteProduct(Long id);
}

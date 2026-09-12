package com.nikyfarm.service;

import com.nikyfarm.dto.CategoryRequestDto;
import com.nikyfarm.dto.CategoryResponseDto;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CategoryService {

    CategoryResponseDto createCategory(CategoryRequestDto dto, MultipartFile file);

    CategoryResponseDto updateCategory(Long id, CategoryRequestDto dto, MultipartFile file);

    CategoryResponseDto getCategoryById(Long id);

    List<CategoryResponseDto> getAllCategories();

    void deleteCategory(Long id);
}

package com.nikyfarm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequestDto {

    @NotBlank(message = "Category name is required")
    @Size(max = 150, message = "Category name must be at most 150 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 160, message = "Slug must be at most 160 characters")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric words separated by hyphens")
    private String slug;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder;

    private Boolean isActive;

    /** Optional parent category id, for building a category tree. Null = top-level category. */
    private Long parentId;
}

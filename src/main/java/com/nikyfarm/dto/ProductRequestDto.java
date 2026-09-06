package com.nikyfarm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 160, message = "Slug must be at most 160 characters")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric words separated by hyphens")
    private String slug;

    @Size(max = 500, message = "Short description must be at most 500 characters")
    private String shortDescription;

    private String description;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", message = "Base price cannot be negative")
    private Double basePrice;

    @DecimalMin(value = "0.0", message = "Discounted price cannot be negative")
    private Double discountedPrice;

    private Boolean isActive;

    @NotNull(message = "Category id is required")
    private Long categoryId;
}

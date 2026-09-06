package com.nikyfarm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductVariantRequestDto {

    @NotBlank(message = "Variant label is required")
    @Size(max = 150, message = "Variant label must be at most 150 characters")
    private String variantLabel;

    private String size;

    private String colour;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private Double price;

    @DecimalMin(value = "0.0", message = "Discounted price cannot be negative")
    private Double discountedPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotBlank(message = "SKU is required")
    @Size(max = 80, message = "SKU must be at most 80 characters")
    private String sku;

    private Boolean isDefault;

    private Boolean isActive;
}

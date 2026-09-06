package com.nikyfarm.dto;

import com.nikyfarm.entity.Attribute;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AttributeRequestDto {

    @NotBlank(message = "Attribute name is required")
    @Size(max = 150, message = "Attribute name must be at most 150 characters")
    private String name;

    @Size(max = 30, message = "Unit must be at most 30 characters")
    private String unit;

    @NotNull(message = "Input type is required")
    private Attribute.InputType inputType;

    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder;

    @NotNull(message = "Category id is required")
    private Long categoryId;
}

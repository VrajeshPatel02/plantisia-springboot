package com.nikyfarm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AttributeOptionRequestDto {

    @NotBlank(message = "Option value is required")
    @Size(max = 150, message = "Option value must be at most 150 characters")
    private String value;
}

package com.nikyfarm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductAttributeValueRequestDto {

    @NotNull(message = "Attribute id is required")
    private Long attributeId;

    /**
     * Required (and validated against the attribute) when the attribute's input type is SELECT;
     * ignored otherwise.
     */
    private Long attributeOptionsId;

    /** Required when the attribute's input type is not SELECT; ignored for SELECT. */
    @Size(max = 500, message = "Value must be at most 500 characters")
    private String valueText;
}

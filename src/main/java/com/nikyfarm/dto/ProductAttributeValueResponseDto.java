package com.nikyfarm.dto;

import com.nikyfarm.entity.Attribute;

public record ProductAttributeValueResponseDto(
        Long id,
        Long productId,
        Long attributeId,
        String attributeName,
        Attribute.InputType inputType,
        Long attributeOptionsId,
        String attributeOptionValue,
        String valueText
) {
}

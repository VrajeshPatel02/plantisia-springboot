package com.nikyfarm.mapper;

import com.nikyfarm.dto.ProductAttributeValueRequestDto;
import com.nikyfarm.dto.ProductAttributeValueResponseDto;
import com.nikyfarm.entity.Attribute;
import com.nikyfarm.entity.AttributeOptions;
import com.nikyfarm.entity.ProductAttributeValue;

public final class ProductAttributeValueMapper {

    private ProductAttributeValueMapper() {}

    public static ProductAttributeValueResponseDto toResponseDto(ProductAttributeValue pav) {
        Attribute attribute = pav.getAttribute();
        AttributeOptions option = pav.getAttributeOptions();
        return new ProductAttributeValueResponseDto(
                pav.getId(),
                pav.getProduct() != null ? pav.getProduct().getId() : null,
                attribute != null ? attribute.getId() : null,
                attribute != null ? attribute.getName() : null,
                attribute != null ? attribute.getInputType() : null,
                option != null ? option.getId() : null,
                option != null ? option.getValue() : null,
                pav.getValueText());
    }

    /** Pre-fills an edit form with the current values of an existing product attribute value. */
    public static ProductAttributeValueRequestDto toRequestDto(ProductAttributeValueResponseDto dto) {
        ProductAttributeValueRequestDto request = new ProductAttributeValueRequestDto();
        request.setAttributeId(dto.attributeId());
        request.setAttributeOptionsId(dto.attributeOptionsId());
        request.setValueText(dto.valueText());
        return request;
    }
}

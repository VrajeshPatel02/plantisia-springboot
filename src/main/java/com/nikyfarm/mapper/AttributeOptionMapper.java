package com.nikyfarm.mapper;

import com.nikyfarm.dto.AttributeOptionRequestDto;
import com.nikyfarm.dto.AttributeOptionResponseDto;
import com.nikyfarm.entity.Attribute;
import com.nikyfarm.entity.AttributeOptions;

public final class AttributeOptionMapper {

    private AttributeOptionMapper() {
    }

    public static AttributeOptionResponseDto toResponseDto(AttributeOptions option) {
        Attribute attribute = option.getAttribute();
        return new AttributeOptionResponseDto(
                option.getId(),
                option.getValue(),
                attribute != null ? attribute.getId() : null,
                attribute != null ? attribute.getName() : null
        );
    }

    /** Pre-fills an edit form with the current values of an existing option. */
    public static AttributeOptionRequestDto toRequestDto(AttributeOptionResponseDto dto) {
        AttributeOptionRequestDto request = new AttributeOptionRequestDto();
        request.setValue(dto.value());
        return request;
    }
}

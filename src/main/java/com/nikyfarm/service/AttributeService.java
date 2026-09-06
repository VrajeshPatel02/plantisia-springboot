package com.nikyfarm.service;

import com.nikyfarm.dto.AttributeRequestDto;
import com.nikyfarm.dto.AttributeResponseDto;

import java.util.List;

public interface AttributeService {

    AttributeResponseDto createAttribute(AttributeRequestDto dto);

    AttributeResponseDto updateAttribute(Long id, AttributeRequestDto dto);

    AttributeResponseDto getAttributeById(Long id);

    /** @param categoryId optional filter; null returns every attribute. */
    List<AttributeResponseDto> getAttributes(Long categoryId);

    void deleteAttribute(Long id);
}

package com.nikyfarm.service;

import com.nikyfarm.dto.AttributeOptionRequestDto;
import com.nikyfarm.dto.AttributeOptionResponseDto;
import java.util.List;

public interface AttributeOptionService {

    AttributeOptionResponseDto createOption(Long attributeId, AttributeOptionRequestDto dto);

    AttributeOptionResponseDto updateOption(Long attributeId, Long optionId, AttributeOptionRequestDto dto);

    AttributeOptionResponseDto getOptionById(Long attributeId, Long optionId);

    List<AttributeOptionResponseDto> getOptionsByAttribute(Long attributeId);

    void deleteOption(Long attributeId, Long optionId);
}

package com.nikyfarm.service;

import com.nikyfarm.dto.AttributeOptionRequestDto;
import com.nikyfarm.dto.AttributeOptionResponseDto;
import com.nikyfarm.entity.Attribute;
import com.nikyfarm.entity.AttributeOptions;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.AttributeOptionMapper;
import com.nikyfarm.repository.AttributeOptionsRepository;
import com.nikyfarm.repository.AttributeRepository;
import com.nikyfarm.repository.ProductAttributeValueRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttributeOptionServiceImpl implements AttributeOptionService {

    private final AttributeOptionsRepository attributeOptionsRepository;
    private final AttributeRepository attributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    @Override
    @Transactional
    public AttributeOptionResponseDto createOption(Long attributeId, AttributeOptionRequestDto dto) {
        Attribute attribute = findAttributeOrThrow(attributeId);
        assertValueAvailable(attributeId, dto.getValue(), null);

        AttributeOptions option = new AttributeOptions();
        option.setAttribute(attribute);
        option.setValue(dto.getValue());
        return AttributeOptionMapper.toResponseDto(attributeOptionsRepository.save(option));
    }

    @Override
    @Transactional
    public AttributeOptionResponseDto updateOption(Long attributeId, Long optionId, AttributeOptionRequestDto dto) {
        AttributeOptions option = findOptionOrThrow(attributeId, optionId);
        assertValueAvailable(attributeId, dto.getValue(), optionId);
        option.setValue(dto.getValue());
        return AttributeOptionMapper.toResponseDto(attributeOptionsRepository.save(option));
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeOptionResponseDto getOptionById(Long attributeId, Long optionId) {
        return AttributeOptionMapper.toResponseDto(findOptionOrThrow(attributeId, optionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttributeOptionResponseDto> getOptionsByAttribute(Long attributeId) {
        findAttributeOrThrow(attributeId);
        return attributeOptionsRepository.findByAttributeId(attributeId).stream()
                .map(AttributeOptionMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteOption(Long attributeId, Long optionId) {
        AttributeOptions option = findOptionOrThrow(attributeId, optionId);
        if (productAttributeValueRepository.existsByAttributeOptionsId(optionId)) {
            throw new BusinessRuleViolationException(
                    "Cannot delete option '" + option.getValue() + "' because products still reference it");
        }
        attributeOptionsRepository.delete(option);
    }

    private void assertValueAvailable(Long attributeId, String value, Long excludeId) {
        boolean taken = excludeId == null
                ? attributeOptionsRepository.existsByAttributeIdAndValueIgnoreCase(attributeId, value)
                : attributeOptionsRepository.existsByAttributeIdAndValueIgnoreCaseAndIdNot(
                        attributeId, value, excludeId);
        if (taken) {
            throw new DuplicateResourceException("Option '" + value + "' already exists for this attribute");
        }
    }

    private Attribute findAttributeOrThrow(Long id) {
        return attributeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with id: " + id));
    }

    private AttributeOptions findOptionOrThrow(Long attributeId, Long optionId) {
        AttributeOptions option = attributeOptionsRepository
                .findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute option not found with id: " + optionId));
        if (option.getAttribute() == null || !option.getAttribute().getId().equals(attributeId)) {
            throw new ResourceNotFoundException("Option " + optionId + " does not belong to attribute " + attributeId);
        }
        return option;
    }
}

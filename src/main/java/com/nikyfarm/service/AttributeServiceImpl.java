package com.nikyfarm.service;

import com.nikyfarm.dto.AttributeRequestDto;
import com.nikyfarm.dto.AttributeResponseDto;
import com.nikyfarm.entity.Attribute;
import com.nikyfarm.entity.Category;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.AttributeMapper;
import com.nikyfarm.repository.AttributeRepository;
import com.nikyfarm.repository.CategoryRepository;
import com.nikyfarm.repository.ProductAttributeValueRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {

    private final AttributeRepository attributeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    @Override
    @Transactional
    public AttributeResponseDto createAttribute(AttributeRequestDto dto) {
        Category category = findCategoryOrThrow(dto.getCategoryId());
        assertNameAvailable(dto.getCategoryId(), dto.getName(), null);

        Attribute attribute = new Attribute();
        applyDto(attribute, dto, category);
        return AttributeMapper.toResponseDto(attributeRepository.save(attribute));
    }

    @Override
    @Transactional
    public AttributeResponseDto updateAttribute(Long id, AttributeRequestDto dto) {
        Attribute attribute = findAttributeOrThrow(id);
        Category category = findCategoryOrThrow(dto.getCategoryId());
        assertNameAvailable(dto.getCategoryId(), dto.getName(), id);

        applyDto(attribute, dto, category);
        return AttributeMapper.toResponseDto(attributeRepository.save(attribute));
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponseDto getAttributeById(Long id) {
        return AttributeMapper.toResponseDto(findAttributeOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponseDto> getAttributes(Long categoryId) {
        List<Attribute> attributes =
                categoryId != null ? attributeRepository.findByCategoryId(categoryId) : attributeRepository.findAll();
        return attributes.stream().map(AttributeMapper::toResponseDto).toList();
    }

    @Override
    @Transactional
    public void deleteAttribute(Long id) {
        Attribute attribute = findAttributeOrThrow(id);
        if (productAttributeValueRepository.existsByAttributeId(id)) {
            throw new BusinessRuleViolationException("Cannot delete attribute '"
                    + attribute.getName()
                    + "' because products still have values set for it");
        }
        // cascade = ALL on Attribute.options removes its own AttributeOptions rows.
        attributeRepository.delete(attribute);
    }

    private void applyDto(Attribute attribute, AttributeRequestDto dto, Category category) {
        attribute.setName(dto.getName());
        attribute.setUnit(dto.getUnit());
        attribute.setInputType(dto.getInputType());
        attribute.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        attribute.setCategory(category);
    }

    private void assertNameAvailable(Long categoryId, String name, Long excludeId) {
        boolean taken = excludeId == null
                ? attributeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, name)
                : attributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, name, excludeId);
        if (taken) {
            throw new DuplicateResourceException("An attribute named '" + name + "' already exists for this category");
        }
    }

    private Category findCategoryOrThrow(Long categoryId) {
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private Attribute findAttributeOrThrow(Long id) {
        return attributeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with id: " + id));
    }
}

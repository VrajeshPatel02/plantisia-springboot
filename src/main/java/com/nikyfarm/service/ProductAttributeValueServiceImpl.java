package com.nikyfarm.service;

import com.nikyfarm.dto.ProductAttributeValueRequestDto;
import com.nikyfarm.dto.ProductAttributeValueResponseDto;
import com.nikyfarm.entity.Attribute;
import com.nikyfarm.entity.AttributeOptions;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ProductAttributeValue;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ProductAttributeValueMapper;
import com.nikyfarm.repository.AttributeOptionsRepository;
import com.nikyfarm.repository.AttributeRepository;
import com.nikyfarm.repository.ProductAttributeValueRepository;
import com.nikyfarm.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigns EAV-style attribute values to a product. Each attribute allows at most one value per
 * product; the shape of the value (free text vs. a picked option) is dictated by the attribute's
 * {@link Attribute.InputType}.
 */
@Service
@RequiredArgsConstructor
public class ProductAttributeValueServiceImpl implements ProductAttributeValueService {

    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final ProductRepository productRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeOptionsRepository attributeOptionsRepository;

    @Override
    @Transactional
    public ProductAttributeValueResponseDto createValue(Long productId, ProductAttributeValueRequestDto dto) {
        Product product = findProductOrThrow(productId);
        Attribute attribute = findAttributeOrThrow(dto.getAttributeId());
        assertAttributeAppliesToProduct(product, attribute);

        if (productAttributeValueRepository.existsByProductIdAndAttributeId(productId, attribute.getId())) {
            throw new DuplicateResourceException(
                    "Product " + productId + " already has a value set for attribute '" + attribute.getName() + "'");
        }

        ProductAttributeValue pav = new ProductAttributeValue();
        pav.setProduct(product);
        applyAndValidateValue(pav, attribute, dto);
        return ProductAttributeValueMapper.toResponseDto(productAttributeValueRepository.save(pav));
    }

    @Override
    @Transactional
    public ProductAttributeValueResponseDto updateValue(
            Long productId, Long valueId, ProductAttributeValueRequestDto dto) {
        Product product = findProductOrThrow(productId);
        ProductAttributeValue pav = findValueOrThrow(productId, valueId);
        Attribute attribute = findAttributeOrThrow(dto.getAttributeId());
        assertAttributeAppliesToProduct(product, attribute);

        if (productAttributeValueRepository.existsByProductIdAndAttributeIdAndIdNot(
                productId, attribute.getId(), valueId)) {
            throw new DuplicateResourceException(
                    "Product " + productId + " already has a value set for attribute '" + attribute.getName() + "'");
        }

        applyAndValidateValue(pav, attribute, dto);
        return ProductAttributeValueMapper.toResponseDto(productAttributeValueRepository.save(pav));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeValueResponseDto getValueById(Long productId, Long valueId) {
        return ProductAttributeValueMapper.toResponseDto(findValueOrThrow(productId, valueId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeValueResponseDto> getValuesByProduct(Long productId) {
        findProductOrThrow(productId);
        return productAttributeValueRepository.findByProductId(productId).stream()
                .map(ProductAttributeValueMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteValue(Long productId, Long valueId) {
        productAttributeValueRepository.delete(findValueOrThrow(productId, valueId));
    }

    /**
     * Sets the attribute and, based on its input type, either the picked option or the free-text
     * value.
     */
    private void applyAndValidateValue(
            ProductAttributeValue pav, Attribute attribute, ProductAttributeValueRequestDto dto) {
        pav.setAttribute(attribute);

        if (attribute.getInputType() == Attribute.InputType.SELECT) {
            if (dto.getAttributeOptionsId() == null) {
                throw new BusinessRuleViolationException(
                        "Attribute '" + attribute.getName() + "' requires selecting an option");
            }
            AttributeOptions option = attributeOptionsRepository
                    .findById(dto.getAttributeOptionsId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Attribute option not found with id: " + dto.getAttributeOptionsId()));
            if (!option.getAttribute().getId().equals(attribute.getId())) {
                throw new BusinessRuleViolationException(
                        "The selected option does not belong to attribute '" + attribute.getName() + "'");
            }
            pav.setAttributeOptions(option);
            pav.setValueText(null);
            return;
        }

        if (dto.getAttributeOptionsId() != null) {
            throw new BusinessRuleViolationException("attributeOptionsId is only applicable to SELECT attributes");
        }
        if (dto.getValueText() == null || dto.getValueText().isBlank()) {
            throw new BusinessRuleViolationException("A value is required for attribute '" + attribute.getName() + "'");
        }

        String value = dto.getValueText().trim();
        if (attribute.getInputType() == Attribute.InputType.NUMBER) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new BusinessRuleViolationException(
                        "Value for attribute '" + attribute.getName() + "' must be numeric");
            }
        } else if (attribute.getInputType() == Attribute.InputType.BOOLEAN) {
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                throw new BusinessRuleViolationException(
                        "Value for attribute '" + attribute.getName() + "' must be true or false");
            }
        }

        pav.setAttributeOptions(null);
        pav.setValueText(value);
    }

    private void assertAttributeAppliesToProduct(Product product, Attribute attribute) {
        Long productCategoryId =
                product.getCategory() != null ? product.getCategory().getId() : null;
        Long attributeCategoryId =
                attribute.getCategory() != null ? attribute.getCategory().getId() : null;
        if (attributeCategoryId != null && !attributeCategoryId.equals(productCategoryId)) {
            throw new BusinessRuleViolationException(
                    "Attribute '" + attribute.getName() + "' does not belong to this product's category");
        }
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private Attribute findAttributeOrThrow(Long attributeId) {
        return attributeRepository
                .findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with id: " + attributeId));
    }

    private ProductAttributeValue findValueOrThrow(Long productId, Long valueId) {
        ProductAttributeValue pav = productAttributeValueRepository
                .findById(valueId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute value not found with id: " + valueId));
        if (pav.getProduct() == null || !pav.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException(
                    "Attribute value " + valueId + " does not belong to product " + productId);
        }
        return pav;
    }
}

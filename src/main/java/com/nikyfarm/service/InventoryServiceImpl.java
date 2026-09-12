package com.nikyfarm.service;

import com.nikyfarm.entity.ProductVariant;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public void deductStock(Long variantId, Integer quantity) {

        validateQuantity(quantity);

        ProductVariant variant = productVariantRepository
                .findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + variantId));

        int updatedRows = productVariantRepository.deductStockIfAvailable(variantId, quantity);

        if (updatedRows == 0) {

            throw new BusinessRuleViolationException("Insufficient stock for variant: " + variant.getVariantLabel());
        }
    }

    @Override
    @Transactional
    public void restoreStock(Long variantId, Integer quantity) {

        validateQuantity(quantity);

        int updatedRows = productVariantRepository.restoreStock(variantId, quantity);

        if (updatedRows == 0) {

            throw new ResourceNotFoundException("Product variant not found: " + variantId);
        }
    }

    private void validateQuantity(Integer quantity) {

        if (quantity == null || quantity <= 0) {

            throw new BusinessRuleViolationException("Quantity must be greater than zero");
        }
    }
}

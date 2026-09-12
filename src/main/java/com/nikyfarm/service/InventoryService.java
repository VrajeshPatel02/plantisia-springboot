package com.nikyfarm.service;

public interface InventoryService {

    void deductStock(Long variantId, Integer quantity);

    void restoreStock(Long variantId, Integer quantity);
}

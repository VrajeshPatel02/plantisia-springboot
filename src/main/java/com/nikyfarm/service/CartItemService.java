package com.nikyfarm.service;

import com.nikyfarm.dto.CartItemRequestDto;
import com.nikyfarm.dto.CartItemResponseDto;

import java.util.List;

public interface CartItemService {

    /** Adds a new cart row, or increments the quantity if this variant is already in the cart. */
    CartItemResponseDto addOrMerge(Long userId, CartItemRequestDto dto);

    List<CartItemResponseDto> getMine(Long userId);

    CartItemResponseDto updateQuantity(Long userId, Long itemId, Integer quantity);

    void remove(Long userId, Long itemId);

    /** Admin: null userId returns every cart item across all users. */
    List<CartItemResponseDto> getAll(Long userId);

    /** Admin: removes any item regardless of owner. */
    void removeByAdmin(Long itemId);
}

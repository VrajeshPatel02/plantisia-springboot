package com.nikyfarm.service;

import com.nikyfarm.dto.CartItemAdminViewDto;
import com.nikyfarm.dto.CartItemRequestDto;
import com.nikyfarm.dto.CartResponseDto;
import java.util.List;

public interface CartService {

    /** Never throws for a user with no cart yet — returns an empty, unpersisted cart view instead. */
    CartResponseDto getCart(Long userId);

    /** Creates the user's cart on first use. Bumps quantity if the variant is already in the cart. */
    CartResponseDto addItem(Long userId, CartItemRequestDto request);

    CartResponseDto updateItemQuantity(Long userId, Long itemId, Integer quantity);

    CartResponseDto removeItem(Long userId, Long itemId);

    void clearCart(Long userId);

    // --- admin-facing (AdminCartItemController) ---

    /**
     * @param userIdFilter optional — null returns items across every user's cart.
     */
    List<CartItemAdminViewDto> listAllItems(Long userIdFilter);

    void deleteItemAsAdmin(Long itemId);
}

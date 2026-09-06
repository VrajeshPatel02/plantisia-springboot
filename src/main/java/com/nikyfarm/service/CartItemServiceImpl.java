package com.nikyfarm.service;

import com.nikyfarm.dto.CartItemRequestDto;
import com.nikyfarm.dto.CartItemResponseDto;
import com.nikyfarm.entity.CartItems;
import com.nikyfarm.entity.ProductVariant;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.CartItemMapper;
import com.nikyfarm.repository.CartItemsRepository;
import com.nikyfarm.repository.ProductVariantRepository;
import com.nikyfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartItemServiceImpl implements CartItemService {

    private final CartItemsRepository cartItemsRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartItemResponseDto addOrMerge(Long userId, CartItemRequestDto dto) {
        ProductVariant variant = findVariantOrThrow(dto.getProductVariantId());

        CartItems cartItem = cartItemsRepository.findByUserIdAndProductVariantId(userId, dto.getProductVariantId())
                .orElseGet(() -> {
                    CartItems fresh = new CartItems();
                    fresh.setUser(findUserOrThrow(userId));
                    fresh.setProductVariant(variant);
                    fresh.setQuantity(0);
                    return fresh;
                });

        int newQuantity = cartItem.getQuantity() + dto.getQuantity();
        assertInStock(variant, newQuantity);
        cartItem.setQuantity(newQuantity);

        return CartItemMapper.toResponseDto(cartItemsRepository.save(cartItem));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponseDto> getMine(Long userId) {
        return cartItemsRepository.findByUserId(userId).stream().map(CartItemMapper::toResponseDto).toList();
    }

    @Override
    @Transactional
    public CartItemResponseDto updateQuantity(Long userId, Long itemId, Integer quantity) {
        CartItems cartItem = findOwnedOrThrow(userId, itemId);
        assertInStock(cartItem.getProductVariant(), quantity);
        cartItem.setQuantity(quantity);
        return CartItemMapper.toResponseDto(cartItemsRepository.save(cartItem));
    }

    @Override
    @Transactional
    public void remove(Long userId, Long itemId) {
        cartItemsRepository.delete(findOwnedOrThrow(userId, itemId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponseDto> getAll(Long userId) {
        List<CartItems> items = userId != null ? cartItemsRepository.findByUserId(userId) : cartItemsRepository.findAll();
        return items.stream().map(CartItemMapper::toResponseDto).toList();
    }

    @Override
    @Transactional
    public void removeByAdmin(Long itemId) {
        CartItems cartItem = cartItemsRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));
        cartItemsRepository.delete(cartItem);
    }

    private void assertInStock(ProductVariant variant, int requestedQuantity) {
        Integer stock = variant.getStockQuantity();
        if (stock != null && requestedQuantity > stock) {
            throw new BusinessRuleViolationException(
                    "Only " + stock + " unit(s) of '" + variant.getVariantLabel() + "' in stock");
        }
    }

    private CartItems findOwnedOrThrow(Long userId, Long itemId) {
        CartItems cartItem = cartItemsRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));
        if (cartItem.getUser() == null || !cartItem.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Cart item not found with id: " + itemId);
        }
        return cartItem;
    }

    private ProductVariant findVariantOrThrow(Long variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + variantId));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}

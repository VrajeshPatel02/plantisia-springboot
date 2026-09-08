package com.nikyfarm.service;

import com.nikyfarm.dto.CartItemAdminViewDto;
import com.nikyfarm.dto.CartItemRequestDto;
import com.nikyfarm.dto.CartResponseDto;
import com.nikyfarm.entity.Cart;
import com.nikyfarm.entity.CartItem;
import com.nikyfarm.entity.ProductVariant;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.CartItemMapper;
import com.nikyfarm.mapper.CartMapper;
import com.nikyfarm.repository.CartItemRepository;
import com.nikyfarm.repository.CartRepository;
import com.nikyfarm.repository.ProductVariantRepository;
import com.nikyfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponseDto getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(CartMapper::toResponseDto)
                .orElseGet(() -> emptyCartView(userId));
    }

    @Override
    @Transactional
    public CartResponseDto addItem(Long userId, CartItemRequestDto request) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProductVariant not found with id: " + request.getProductVariantId()));

        CartItem existing = cartItemRepository
                .findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem(cart, variant, request.getQuantity());
            cart.addItem(item);
            cartItemRepository.save(item);
        }

        return CartMapper.toResponseDto(reloadCart(userId));
    }

    @Override
    @Transactional
    public CartResponseDto updateItemQuantity(Long userId, Long itemId, Integer quantity) {
        Cart cart = requireCart(userId);
        CartItem item = requireItem(cart, itemId);

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return CartMapper.toResponseDto(reloadCart(userId));
    }

    @Override
    @Transactional
    public CartResponseDto removeItem(Long userId, Long itemId) {
        Cart cart = requireCart(userId);
        CartItem item = requireItem(cart, itemId);

        cart.removeItem(item);
        cartItemRepository.delete(item);

        return cartRepository.findByUserId(userId)
                .map(CartMapper::toResponseDto)
                .orElseGet(() -> emptyCartView(userId));
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId)
                .ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemAdminViewDto> listAllItems(Long userIdFilter) {
        List<CartItem> items = userIdFilter != null
                ? cartItemRepository.findByCart_User_IdOrderByAddedAtDesc(userIdFilter)
                : cartItemRepository.findAllByOrderByAddedAtDesc();

        return items.stream()
                .map(CartItemMapper::toAdminViewDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteItemAsAdmin(Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        Cart cart = item.getCart();
        if (cart != null) {
            cart.removeItem(item);
        }
        cartItemRepository.delete(item);
    }

    // --- helpers ---

    /** Lazily creates the Cart row on first "add to cart" — never at registration time. */
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
                    return cartRepository.save(new Cart(user));
                });
    }

    private Cart requireCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
    }

    private CartItem requireItem(Cart cart, Long itemId) {
        return cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));
    }

    /** Re-reads via the @EntityGraph query so the returned DTO has fresh item/variant/product data. */
    private Cart reloadCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
    }

    private CartResponseDto emptyCartView(Long userId) {
        return CartResponseDto.builder()
                .id(null)
                .userId(userId)
                .items(List.of())
                .totalItemCount(0)
                .totalPrice(BigDecimal.ZERO)
                .build();
    }
}

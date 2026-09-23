package com.nikyfarm.service;

import com.nikyfarm.dto.CartItemRequestDto;
import com.nikyfarm.dto.CartItemResponseDto;
import com.nikyfarm.entity.Cart;
import com.nikyfarm.entity.CartItem;
import com.nikyfarm.entity.ProductVariant;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.CartItemMapper;
import com.nikyfarm.repository.CartItemRepository;
import com.nikyfarm.repository.CartRepository;
import com.nikyfarm.repository.ProductVariantRepository;
import com.nikyfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartItemServiceImpl implements CartItemService {

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    // =========================================================================
    // CUSTOMER
    // =========================================================================

    @Override
    @Transactional
    public CartItemResponseDto addOrMerge(
            Long userId,
            CartItemRequestDto dto
    ) {
        validateQuantity(dto.getQuantity());

        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = findVariantOrThrow(
                dto.getProductVariantId()
        );

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductVariantId(
                        cart.getId(),
                        variant.getId()
                )
                .orElse(null);

        if (cartItem == null) {

            assertInStock(
                    variant,
                    dto.getQuantity()
            );

            cartItem = new CartItem(
                    cart,
                    variant,
                    dto.getQuantity()
            );

        } else {

            int newQuantity =
                    cartItem.getQuantity() + dto.getQuantity();

            assertInStock(
                    variant,
                    newQuantity
            );

            cartItem.setQuantity(newQuantity);
        }

        return CartItemMapper.toResponseDto(
                cartItemRepository.save(cartItem)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponseDto> getMine(Long userId) {

        Cart cart = cartRepository
                .findByUserId(userId)
                .orElse(null);

        if (cart == null) {
            return List.of();
        }

        return cartItemRepository
                .findByCartId(cart.getId())
                .stream()
                .map(CartItemMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public CartItemResponseDto updateQuantity(
            Long userId,
            Long itemId,
            Integer quantity
    ) {
        validateQuantity(quantity);

        Cart cart = requireCart(userId);

        CartItem cartItem = cartItemRepository
                .findByIdAndCartId(
                        itemId,
                        cart.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found with id: " + itemId
                        )
                );

        assertInStock(
                cartItem.getProductVariant(),
                quantity
        );

        cartItem.setQuantity(quantity);

        return CartItemMapper.toResponseDto(
                cartItemRepository.save(cartItem)
        );
    }

    @Override
    @Transactional
    public void remove(
            Long userId,
            Long itemId
    ) {
        Cart cart = requireCart(userId);

        CartItem cartItem = cartItemRepository
                .findByIdAndCartId(
                        itemId,
                        cart.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found with id: " + itemId
                        )
                );

        cartItemRepository.delete(cartItem);
    }

    // =========================================================================
    // ADMIN
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponseDto> getAll(Long userId) {

        List<CartItem> items;

        if (userId != null) {

            items = cartItemRepository
                    .findByCart_User_IdOrderByAddedAtDesc(userId);

        } else {

            items = cartItemRepository
                    .findAllByOrderByAddedAtDesc();
        }

        return items
                .stream()
                .map(CartItemMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void removeByAdmin(Long itemId) {

        CartItem cartItem = cartItemRepository
                .findById(itemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found with id: " + itemId
                        )
                );

        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {

        Cart cart = requireCart(userId);

        cartItemRepository.deleteByCartId(cart.getId());
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Gets the user's existing cart or creates it lazily.
     *
     * A cart is created only when the user first adds something.
     */
    private Cart getOrCreateCart(Long userId) {

        return cartRepository
                .findByUserId(userId)
                .orElseGet(() -> {

                    User user = userRepository
                            .findById(userId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "User not found with id: " + userId
                                    )
                            );

                    return cartRepository.save(
                            new Cart(user)
                    );
                });
    }

    /**
     * Finds the user's cart.
     */
    private Cart requireCart(Long userId) {

        return cartRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart not found for user id: " + userId
                        )
                );
    }

    /**
     * Finds a product variant or throws 404.
     */
    private ProductVariant findVariantOrThrow(
            Long variantId
    ) {

        return productVariantRepository
                .findById(variantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product variant not found with id: "
                                        + variantId
                        )
                );
    }

    /**
     * Validates that requested quantity is positive.
     */
    private void validateQuantity(Integer quantity) {

        if (quantity == null || quantity < 1) {
            throw new BusinessRuleViolationException(
                    "Quantity must be at least 1"
            );
        }
    }

    /**
     * Prevents adding/updating a cart item beyond available stock.
     */
    private void assertInStock(
            ProductVariant variant,
            int requestedQuantity
    ) {

        Integer stock = variant.getStockQuantity();

        if (stock != null && requestedQuantity > stock) {

            throw new BusinessRuleViolationException(
                    "Only "
                            + stock
                            + " unit(s) of '"
                            + variant.getVariantLabel()
                            + "' in stock"
            );
        }
    }
}
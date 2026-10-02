package com.nikyfarm.service;

import com.nikyfarm.dto.CartItemAdminViewDto;
import com.nikyfarm.dto.CartItemRequestDto;
import com.nikyfarm.dto.CartResponseDto;
import com.nikyfarm.entity.Cart;
import com.nikyfarm.entity.CartItem;
import com.nikyfarm.entity.ProductVariant;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.CartItemMapper;
import com.nikyfarm.mapper.CartMapper;
import com.nikyfarm.repository.CartItemRepository;
import com.nikyfarm.repository.CartRepository;
import com.nikyfarm.repository.ProductVariantRepository;
import com.nikyfarm.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        return cartRepository
                .findByUserId(userId)
                .map(CartMapper::toResponseDto)
                .orElseGet(() -> emptyCartView(userId));
    }

    @Override
    @Transactional
    public CartResponseDto addItem(Long userId, CartItemRequestDto request) {

        validateRequest(request);

        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = productVariantRepository
                .findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProductVariant not found with id: " + request.getProductVariantId()));

        CartItem existing = cartItemRepository
                .findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                .orElse(null);

        int requestedQuantity = request.getQuantity();

        int totalQuantity = existing != null ? existing.getQuantity() + requestedQuantity : requestedQuantity;

        validateCartVariant(variant, totalQuantity);

        if (existing != null) {

            existing.setQuantity(totalQuantity);

            cartItemRepository.save(existing);

        } else {

            CartItem item = new CartItem(cart, variant, requestedQuantity);

            cart.addItem(item);

            cartItemRepository.save(item);
        }

        return CartMapper.toResponseDto(reloadCart(userId));
    }

    @Override
    @Transactional
    public CartResponseDto updateItemQuantity(Long userId, Long itemId, Integer quantity) {

        validateQuantity(quantity);

        Cart cart = requireCart(userId);

        CartItem item = requireItem(cart, itemId);

        ProductVariant variant = item.getProductVariant();

        if (variant == null) {

            throw new BusinessRuleViolationException("Product variant is no longer available");
        }

        validateCartVariant(variant, quantity);

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

        return cartRepository
                .findByUserId(userId)
                .map(CartMapper::toResponseDto)
                .orElseGet(() -> emptyCartView(userId));
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartItemRepository.deleteAllByCartId(cart.getId());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemAdminViewDto> listAllItems(Long userIdFilter) {

        List<CartItem> items = userIdFilter != null
                ? cartItemRepository.findByCart_User_IdOrderByAddedAtDesc(userIdFilter)
                : cartItemRepository.findAllByOrderByAddedAtDesc();

        return items.stream().map(CartItemMapper::toAdminViewDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteItemAsAdmin(Long itemId) {

        CartItem item = cartItemRepository
                .findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        Cart cart = item.getCart();

        if (cart != null) {

            cart.removeItem(item);
        }

        cartItemRepository.delete(item);
    }

    // =========================
    // Validation
    // =========================

    private void validateRequest(CartItemRequestDto request) {

        if (request == null) {

            throw new BusinessRuleViolationException("Cart item request is required");
        }

        if (request.getProductVariantId() == null) {

            throw new BusinessRuleViolationException("Product variant ID is required");
        }

        validateQuantity(request.getQuantity());
    }

    private void validateQuantity(Integer quantity) {

        if (quantity == null || quantity <= 0) {

            throw new BusinessRuleViolationException("Quantity must be greater than zero");
        }
    }

    /**
     * Validates whether a variant can currently be added or updated in the cart.
     *
     * <p>Stock is NOT deducted or reserved here. Checkout performs the final atomic stock deduction.
     */
    private void validateCartVariant(ProductVariant variant, Integer requestedQuantity) {

        validateQuantity(requestedQuantity);

        if (!Boolean.TRUE.equals(variant.getIsActive())) {

            throw new BusinessRuleViolationException("This product variant is currently unavailable");
        }

        if (variant.getProduct() == null) {

            throw new BusinessRuleViolationException("Product is currently unavailable");
        }

        if (!Boolean.TRUE.equals(variant.getProduct().getIsActive())) {

            throw new BusinessRuleViolationException("This product is currently unavailable");
        }

        Integer availableStock = variant.getStockQuantity();

        if (availableStock == null || availableStock <= 0) {

            throw new BusinessRuleViolationException("This product variant is out of stock");
        }

        if (requestedQuantity > availableStock) {

            throw new BusinessRuleViolationException(
                    "Requested quantity exceeds available stock. " + "Available stock: " + availableStock);
        }
    }

    // =========================
    // Helpers
    // =========================

    /** Lazily creates the Cart row on first "add to cart". Never at registration time. */
    private Cart getOrCreateCart(Long userId) {

        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository
                    .findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            return cartRepository.save(new Cart(user));
        });
    }

    private Cart requireCart(Long userId) {

        return cartRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
    }

    private CartItem requireItem(Cart cart, Long itemId) {

        return cartItemRepository
                .findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));
    }

    /**
     * Re-reads via the @EntityGraph query so the returned DTO has fresh item/variant/product data.
     */
    private Cart reloadCart(Long userId) {

        return cartRepository
                .findByUserId(userId)
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

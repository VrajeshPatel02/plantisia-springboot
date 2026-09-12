package com.nikyfarm.controller;

import com.nikyfarm.dto.CartItemQuantityUpdateDto;
import com.nikyfarm.dto.CartItemRequestDto;
import com.nikyfarm.dto.CartResponseDto;
import com.nikyfarm.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Current user's own cart — mirrors the shape of WishlistController. Replaces the old
 * CartItemController; the resource is now the aggregate ("my cart"), with items as a nested
 * collection under it, rather than flat cart-item CRUD.
 */
@RestController
@RequestMapping("/api/me/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponseDto> getCart(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDto> addItem(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody CartItemRequestDto request) {
        CartResponseDto cart = cartService.addItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> updateItemQuantity(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemQuantityUpdateDto request) {
        CartResponseDto cart = cartService.updateItemQuantity(userId, itemId, request.getQuantity());
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> removeItem(@AuthenticationPrincipal Long userId, @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(userId, itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}

package com.nikyfarm.controller;

import com.nikyfarm.dto.CartItemRequestDto;
import com.nikyfarm.dto.CartItemResponseDto;
import com.nikyfarm.service.CartItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me/cart")
@RequiredArgsConstructor
@Validated
public class CartItemController {

    private final CartItemService cartItemService;

    @GetMapping
    public ResponseEntity<List<CartItemResponseDto>> getMine(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(cartItemService.getMine(userId));
    }

    @PostMapping
    public ResponseEntity<CartItemResponseDto> add(@AuthenticationPrincipal Long userId,
                                                    @RequestBody @Valid CartItemRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartItemService.addOrMerge(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartItemResponseDto> updateQuantity(@AuthenticationPrincipal Long userId,
                                                               @PathVariable Long id,
                                                               @RequestParam @Min(1) Integer quantity) {
        return ResponseEntity.ok(cartItemService.updateQuantity(userId, id, quantity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        cartItemService.remove(userId, id);
        return ResponseEntity.noContent().build();
    }
}

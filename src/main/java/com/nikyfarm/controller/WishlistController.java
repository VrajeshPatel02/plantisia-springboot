package com.nikyfarm.controller;

import com.nikyfarm.dto.WishlistRequestDto;
import com.nikyfarm.dto.WishlistResponseDto;
import com.nikyfarm.service.WishlistService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistResponseDto>> getMine(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(wishlistService.getMine(userId));
    }

    @PostMapping
    public ResponseEntity<WishlistResponseDto> add(
            @AuthenticationPrincipal Long userId, @RequestBody @Valid WishlistRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wishlistService.add(userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        wishlistService.remove(userId, id);
        return ResponseEntity.noContent().build();
    }
}

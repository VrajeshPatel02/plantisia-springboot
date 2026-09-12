package com.nikyfarm.controller;

import com.nikyfarm.dto.OrderCreateRequestDto;
import com.nikyfarm.dto.OrderResponseDto;
import com.nikyfarm.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> checkoutFromCart(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long shippingAddressId,
            @RequestParam(required = false) Long billingAddressId,
            @RequestParam(required = false) String couponCode) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrderFromCart(userId, shippingAddressId, billingAddressId, couponCode));
    }

    @PostMapping("/buy-now")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> buyNow(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody OrderCreateRequestDto request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createDirectOrder(userId, request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponseDto>> getMyOrders(
            @AuthenticationPrincipal Long userId, @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(orderService.getUserOrders(userId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> getOrderById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {

        return ResponseEntity.ok(orderService.getOrderById(id, userId, false));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> cancelOrder(@AuthenticationPrincipal Long userId, @PathVariable Long id) {

        return ResponseEntity.ok(orderService.cancelOrder(id, userId, false));
    }
}

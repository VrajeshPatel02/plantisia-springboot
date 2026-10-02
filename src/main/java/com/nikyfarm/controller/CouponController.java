package com.nikyfarm.controller;

import com.nikyfarm.dto.CouponResponseDto;
import com.nikyfarm.dto.CouponValidationRequestDto;
import com.nikyfarm.dto.CouponValidationResponseDto;
import com.nikyfarm.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * Returns coupons available for the authenticated customer.
     * <p>
     * The frontend uses this endpoint to display:
     * - coupon code
     * - description
     * - minimum order value
     * - discount information
     * - active/inactive state
     * - validity dates
     * - usage information
     */
    @GetMapping
    public ResponseEntity<Page<CouponResponseDto>> getAllCoupons(
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                couponService.getAllCoupons(pageable)
        );
    }

    /**
     * Validates a coupon against the current checkout subtotal.
     * <p>
     * Backend remains authoritative for:
     * - active status
     * - validity dates
     * - usage limit
     * - minimum order value
     * - discount calculation
     */
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponseDto> validateCoupon(
            @Valid @RequestBody CouponValidationRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                couponService.validateCoupon(requestDto)
        );
    }
}
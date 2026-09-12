package com.nikyfarm.controller;

import com.nikyfarm.dto.CouponValidationRequestDto;
import com.nikyfarm.dto.CouponValidationResponseDto;
import com.nikyfarm.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponseDto> validateCoupon(
            @Valid @RequestBody CouponValidationRequestDto requestDto) {
        return ResponseEntity.ok(couponService.validateCoupon(requestDto));
    }
}

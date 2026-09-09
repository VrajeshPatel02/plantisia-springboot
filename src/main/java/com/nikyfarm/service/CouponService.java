package com.nikyfarm.service;

import com.nikyfarm.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponService {
    CouponResponseDto createCoupon(CouponRequestDto requestDto);
    CouponResponseDto updateCoupon(Long id, CouponRequestDto requestDto);
    CouponResponseDto getCouponById(Long id);
    CouponResponseDto getCouponByCode(String code);
    Page<CouponResponseDto> getAllCoupons(Pageable pageable);
    CouponValidationResponseDto validateCoupon(CouponValidationRequestDto requestDto);
    void toggleCouponStatus(Long id);
    void deleteCoupon(Long id);
}
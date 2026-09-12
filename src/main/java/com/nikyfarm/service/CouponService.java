package com.nikyfarm.service;

import com.nikyfarm.dto.*;
import com.nikyfarm.entity.Coupon;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponService {

    CouponResponseDto createCoupon(CouponRequestDto requestDto);

    CouponResponseDto updateCoupon(Long id, CouponRequestDto requestDto);

    CouponResponseDto getCouponById(Long id);

    CouponResponseDto getCouponByCode(String code);

    Page<CouponResponseDto> getAllCoupons(Pageable pageable);

    CouponValidationResponseDto validateCoupon(CouponValidationRequestDto requestDto);

    Coupon validateCouponForOrder(String couponCode, BigDecimal orderSubtotal);

    void consumeCoupon(Long couponId);

    void toggleCouponStatus(Long id);

    void deleteCoupon(Long id);

    void consumeCouponUsage(Long couponId);
}

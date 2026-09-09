package com.nikyfarm.service;

import com.nikyfarm.dto.*;
import com.nikyfarm.entity.Coupon;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.CouponMapper;
import com.nikyfarm.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public CouponResponseDto createCoupon(CouponRequestDto requestDto) {
        String formattedCode = requestDto.getCode().toUpperCase().trim();
        if (couponRepository.existsByCode(formattedCode)) {
            throw new BusinessRuleViolationException("Coupon code already exists: " + formattedCode);
        }

        Coupon coupon = CouponMapper.toEntity(requestDto);
        Coupon savedCoupon = couponRepository.save(coupon);
        return CouponMapper.toResponseDto(savedCoupon);
    }

    @Override
    @Transactional
    public CouponResponseDto updateCoupon(Long id, CouponRequestDto requestDto) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));

        String formattedCode = requestDto.getCode().toUpperCase().trim();
        if (!coupon.getCode().equalsIgnoreCase(formattedCode) && couponRepository.existsByCode(formattedCode)) {
            throw new BusinessRuleViolationException("Coupon code already exists: " + formattedCode);
        }

        coupon.setCode(formattedCode);
        coupon.setDescription(requestDto.getDescription());
        coupon.setDiscountType(requestDto.getDiscountType());
        coupon.setDiscountValue(requestDto.getDiscountValue());
        coupon.setMinOrderValue(requestDto.getMinOrderValue() != null ? requestDto.getMinOrderValue() : BigDecimal.ZERO);
        coupon.setMaxUses(requestDto.getMaxUses());
        coupon.setValidFrom(requestDto.getValidFrom());
        coupon.setValidUntil(requestDto.getValidUntil());
        if (requestDto.getIsActive() != null) {
            coupon.setIsActive(requestDto.getIsActive());
        }

        Coupon updatedCoupon = couponRepository.save(coupon);
        return CouponMapper.toResponseDto(updatedCoupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponseDto getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));
        return CouponMapper.toResponseDto(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponseDto getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));
        return CouponMapper.toResponseDto(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CouponResponseDto> getAllCoupons(Pageable pageable) {
        return couponRepository.findAllByOrderByValidUntilDesc(pageable)
                .map(CouponMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponseDto validateCoupon(CouponValidationRequestDto requestDto) {
        String code = requestDto.getCode().toUpperCase().trim();
        Coupon coupon = couponRepository.findByCode(code).orElse(null);

        if (coupon == null) {
            return CouponValidationResponseDto.builder()
                    .valid(false)
                    .code(code)
                    .discountAmount(BigDecimal.ZERO)
                    .message("Invalid coupon code")
                    .build();
        }

        if (!coupon.isValid(requestDto.getOrderSubtotal())) {
            String message = "Coupon is invalid or expired";
            if (coupon.getMinOrderValue() != null && requestDto.getOrderSubtotal().compareTo(coupon.getMinOrderValue()) < 0) {
                message = "Minimum order value of ₹" + coupon.getMinOrderValue() + " required";
            }
            return CouponValidationResponseDto.builder()
                    .valid(false)
                    .code(code)
                    .discountAmount(BigDecimal.ZERO)
                    .message(message)
                    .build();
        }

        BigDecimal discountAmount = coupon.calculateDiscount(requestDto.getOrderSubtotal());

        return CouponValidationResponseDto.builder()
                .valid(true)
                .code(code)
                .discountAmount(discountAmount)
                .message("Coupon applied successfully")
                .build();
    }

    @Override
    @Transactional
    public void toggleCouponStatus(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));
        coupon.setIsActive(!Boolean.TRUE.equals(coupon.getIsActive()));
        couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon not found with ID: " + id);
        }
        couponRepository.deleteById(id);
    }
}
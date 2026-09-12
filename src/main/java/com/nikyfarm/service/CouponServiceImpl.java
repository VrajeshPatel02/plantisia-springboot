package com.nikyfarm.service;

import com.nikyfarm.dto.*;
import com.nikyfarm.entity.Coupon;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.CouponMapper;
import com.nikyfarm.repository.CouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public CouponResponseDto createCoupon(CouponRequestDto requestDto) {

        validateCouponConfiguration(requestDto);

        String formattedCode = normalizeCode(requestDto.getCode());

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

        validateCouponConfiguration(requestDto);

        Coupon coupon = couponRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));

        String formattedCode = normalizeCode(requestDto.getCode());

        if (!coupon.getCode().equalsIgnoreCase(formattedCode) && couponRepository.existsByCode(formattedCode)) {

            throw new BusinessRuleViolationException("Coupon code already exists: " + formattedCode);
        }

        coupon.setCode(formattedCode);

        coupon.setDescription(requestDto.getDescription());

        coupon.setDiscountType(requestDto.getDiscountType());

        coupon.setDiscountValue(requestDto.getDiscountValue());

        coupon.setMinOrderValue(
                requestDto.getMinOrderValue() != null ? requestDto.getMinOrderValue() : BigDecimal.ZERO);

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

        Coupon coupon = couponRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));

        return CouponMapper.toResponseDto(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponseDto getCouponByCode(String code) {

        String formattedCode = normalizeCode(code);

        Coupon coupon = couponRepository
                .findByCode(formattedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + formattedCode));

        return CouponMapper.toResponseDto(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CouponResponseDto> getAllCoupons(Pageable pageable) {

        return couponRepository.findAllByOrderByValidUntilDesc(pageable).map(CouponMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponseDto validateCoupon(CouponValidationRequestDto requestDto) {

        if (requestDto == null) {

            throw new BusinessRuleViolationException("Coupon validation request is required");
        }

        validateOrderSubtotal(requestDto.getOrderSubtotal());

        String formattedCode = normalizeCode(requestDto.getCode());

        Coupon coupon = couponRepository.findByCode(formattedCode).orElse(null);

        if (coupon == null) {

            return invalidResponse(formattedCode, "Invalid coupon code");
        }

        String validationError = getValidationError(coupon, requestDto.getOrderSubtotal());

        if (validationError != null) {

            return invalidResponse(formattedCode, validationError);
        }

        BigDecimal discountAmount = coupon.calculateDiscount(requestDto.getOrderSubtotal());

        return CouponValidationResponseDto.builder()
                .valid(true)
                .code(formattedCode)
                .discountAmount(discountAmount)
                .message("Coupon applied successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon validateCouponForOrder(String couponCode, BigDecimal orderSubtotal) {

        validateOrderSubtotal(orderSubtotal);

        String formattedCode = normalizeCode(couponCode);

        Coupon coupon = couponRepository
                .findByCode(formattedCode)
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid coupon code"));

        String validationError = getValidationError(coupon, orderSubtotal);

        if (validationError != null) {

            throw new BusinessRuleViolationException(validationError);
        }

        return coupon;
    }

    @Override
    @Transactional
    public void consumeCoupon(Long couponId) {

        if (couponId == null) {

            throw new BusinessRuleViolationException("Coupon ID is required");
        }

        int updatedRows = couponRepository.incrementUsageIfAvailable(couponId);

        if (updatedRows == 0) {

            throw new BusinessRuleViolationException("Coupon usage limit has been reached");
        }
    }

    @Override
    @Transactional
    public void toggleCouponStatus(Long id) {

        Coupon coupon = couponRepository
                .findById(id)
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

    @Override
    @Transactional
    public void consumeCouponUsage(Long couponId) {

        int updatedRows = couponRepository.consumeCouponUsage(couponId, LocalDateTime.now());

        if (updatedRows == 0) {

            throw new BusinessRuleViolationException("Coupon can no longer be consumed");
        }
    }

    // =========================================
    // Validation Helpers
    // =========================================

    private void validateCouponConfiguration(CouponRequestDto requestDto) {

        if (requestDto == null) {

            throw new BusinessRuleViolationException("Coupon request is required");
        }

        if (requestDto.getDiscountType() == null) {

            throw new BusinessRuleViolationException("Discount type is required");
        }

        if (requestDto.getDiscountValue() == null
                || requestDto.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessRuleViolationException("Discount value must be greater than zero");
        }

        if (requestDto.getDiscountType() == Coupon.DiscountType.PERCENTAGE
                && requestDto.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new BusinessRuleViolationException("Percentage discount cannot exceed 100");
        }

        if (requestDto.getMinOrderValue() != null
                && requestDto.getMinOrderValue().compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException("Minimum order value cannot be negative");
        }

        if (requestDto.getMaxUses() != null && requestDto.getMaxUses() <= 0) {

            throw new BusinessRuleViolationException("Maximum uses must be greater than zero");
        }

        LocalDateTime validFrom = requestDto.getValidFrom();

        LocalDateTime validUntil = requestDto.getValidUntil();

        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {

            throw new BusinessRuleViolationException("Valid until date cannot be before valid from date");
        }
    }

    private void validateOrderSubtotal(BigDecimal orderSubtotal) {

        if (orderSubtotal == null || orderSubtotal.compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException("Order subtotal cannot be negative");
        }
    }

    private String getValidationError(Coupon coupon, BigDecimal orderSubtotal) {

        if (!Boolean.TRUE.equals(coupon.getIsActive())) {

            return "Coupon is inactive";
        }

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {

            return "Coupon is not active yet";
        }

        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {

            return "Coupon has expired";
        }

        int timesUsed = coupon.getTimesUsed() != null ? coupon.getTimesUsed() : 0;

        if (coupon.getMaxUses() != null && timesUsed >= coupon.getMaxUses()) {

            return "Coupon usage limit has been reached";
        }

        if (coupon.getMinOrderValue() != null && orderSubtotal.compareTo(coupon.getMinOrderValue()) < 0) {

            return "Minimum order value of ₹" + coupon.getMinOrderValue() + " required";
        }

        return null;
    }

    private CouponValidationResponseDto invalidResponse(String code, String message) {

        return CouponValidationResponseDto.builder()
                .valid(false)
                .code(code)
                .discountAmount(BigDecimal.ZERO)
                .message(message)
                .build();
    }

    private String normalizeCode(String code) {

        if (code == null || code.isBlank()) {

            throw new BusinessRuleViolationException("Coupon code is required");
        }

        return code.trim().toUpperCase();
    }
}

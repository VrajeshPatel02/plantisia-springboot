package com.nikyfarm.mapper;

import com.nikyfarm.dto.CouponRequestDto;
import com.nikyfarm.dto.CouponResponseDto;
import com.nikyfarm.entity.Coupon;

import java.math.BigDecimal;

public class CouponMapper {

    public static Coupon toEntity(CouponRequestDto dto) {
        if (dto == null) return null;

        return Coupon.builder()
                .code(dto.getCode().toUpperCase().trim())
                .description(dto.getDescription())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .minOrderValue(dto.getMinOrderValue() != null ? dto.getMinOrderValue() : BigDecimal.ZERO)
                .maxUses(dto.getMaxUses())
                .validFrom(dto.getValidFrom())
                .validUntil(dto.getValidUntil())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .timesUsed(0)
                .build();
    }

    public static CouponResponseDto toResponseDto(Coupon coupon) {
        if (coupon == null) return null;

        return CouponResponseDto.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxUses(coupon.getMaxUses())
                .timesUsed(coupon.getTimesUsed())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .isActive(coupon.getIsActive())
                .isValid(coupon.isValid(BigDecimal.ZERO))
                .build();
    }
}
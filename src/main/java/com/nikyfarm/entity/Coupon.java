package com.nikyfarm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupons_code", columnList = "code")
})
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Builder.Default
    @Column(name = "min_order_value", precision = 10, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Builder.Default
    @Column(name = "times_used")
    private Integer timesUsed = 0;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public enum DiscountType {
        PERCENTAGE, FLAT
    }

    public boolean isValid(BigDecimal orderSubtotal) {
        if (Boolean.FALSE.equals(isActive)) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }

        if (validUntil != null && now.isAfter(validUntil)) {
            return false;
        }

        if (maxUses != null && timesUsed >= maxUses) {
            return false;
        }

        if (minOrderValue != null && orderSubtotal.compareTo(minOrderValue) < 0) {
            return false;
        }

        return true;
    }

    public BigDecimal calculateDiscount(BigDecimal orderSubtotal) {
        if (!isValid(orderSubtotal)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderSubtotal.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (discountType == DiscountType.FLAT) {
            discount = discountValue;
        }

        // Discount cannot exceed subtotal
        if (discount.compareTo(orderSubtotal) > 0) {
            return orderSubtotal;
        }

        return discount;
    }

    public void incrementTimesUsed() {
        if (this.timesUsed == null) {
            this.timesUsed = 0;
        }
        this.timesUsed++;
    }
}
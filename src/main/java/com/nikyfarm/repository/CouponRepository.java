package com.nikyfarm.repository;

import com.nikyfarm.entity.Coupon;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    Page<Coupon> findAllByOrderByValidUntilDesc(Pageable pageable);

    @Modifying
    @Query(
            """
            update Coupon c
            set c.timesUsed =
                    coalesce(c.timesUsed, 0) + 1
            where c.id = :couponId
            and (
                    c.maxUses is null
                    or coalesce(c.timesUsed, 0) < c.maxUses
            )
            """)
    int incrementUsageIfAvailable(@Param("couponId") Long couponId);

    /*
     * Atomically consume one coupon usage.
     *
     * Conditions:
     *
     * - Coupon must be active
     * - Current time must be within validity range
     * - maxUses must not already be reached
     *
     * If maxUses is null,
     * usage is unlimited.
     *
     * Returns:
     *
     * 1 = Coupon consumed successfully
     * 0 = Coupon cannot be consumed
     */
    @Modifying
    @Query(
            """
            update Coupon c
            set c.timesUsed = c.timesUsed + 1
            where c.id = :couponId
            and c.isActive = true
            and (
                c.validFrom is null
                or c.validFrom <= :now
            )
            and (
                c.validUntil is null
                or c.validUntil >= :now
            )
            and (
                c.maxUses is null
                or c.timesUsed < c.maxUses
            )
            """)
    int consumeCouponUsage(@Param("couponId") Long couponId, @Param("now") LocalDateTime now);
}

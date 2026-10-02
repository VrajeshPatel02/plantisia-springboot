package com.nikyfarm.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_order", columnList = "order_id"),
                @Index(name = "idx_payments_provider_order", columnList = "provider_order_id"),
                @Index(name = "idx_payments_provider_payment", columnList = "provider_payment_id")
        })
public class Payments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // ORDER
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // =========================================================
    // PROVIDER
    // =========================================================

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_order_id")
    private String providerOrderId;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(name = "provider_refund_id")
    private String providerRefundId;

    // =========================================================
    // PAYMENT DETAILS
    // =========================================================

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false)
    private String currency;

    // =========================================================
    // STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    // =========================================================
    // FAILURE INFORMATION
    // =========================================================

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    // =========================================================
    // TIMESTAMPS
    // =========================================================

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =========================================================
    // LIFECYCLE
    // =========================================================

    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();

        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // ENUM
    // =========================================================

    public enum Status {
        CREATED,

        PENDING,

        CAPTURED,

        SUCCESS,

        REFUND_PENDING,

        FAILED,

        REFUNDED
    }
}

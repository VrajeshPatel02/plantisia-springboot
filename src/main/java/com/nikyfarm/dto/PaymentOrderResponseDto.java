package com.nikyfarm.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentOrderResponseDto {

    private Long paymentId;

    private Long orderId;

    private String orderNumber;

    private String razorpayOrderId;

    private String razorpayKeyId;

    private BigDecimal amount;

    private String currency;
}

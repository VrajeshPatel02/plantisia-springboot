package com.nikyfarm.service;

import com.nikyfarm.dto.CreatePaymentRequestDto;
import com.nikyfarm.dto.PaymentOrderResponseDto;
import com.nikyfarm.dto.VerifyPaymentRequestDto;

public interface PaymentService {

    PaymentOrderResponseDto createRazorpayOrder(Long userId, CreatePaymentRequestDto request);

    void verifyRazorpayPayment(Long userId, VerifyPaymentRequestDto request);

    void processRazorpayWebhook(String payload, String webhookSignature);

    void refundOrderPayment(Long orderId);
}

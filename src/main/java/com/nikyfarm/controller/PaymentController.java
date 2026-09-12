package com.nikyfarm.controller;

import com.nikyfarm.dto.CreatePaymentRequestDto;
import com.nikyfarm.dto.PaymentOrderResponseDto;
import com.nikyfarm.dto.VerifyPaymentRequestDto;
import com.nikyfarm.dto.WebhookResponseDto;
import com.nikyfarm.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders")
    public ResponseEntity<PaymentOrderResponseDto> createRazorpayOrder(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody CreatePaymentRequestDto request) {

        PaymentOrderResponseDto response = paymentService.createRazorpayOrder(userId, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyPayment(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody VerifyPaymentRequestDto request) {

        paymentService.verifyRazorpayPayment(userId, request);

        return ResponseEntity.ok().build();
    }

    /*
     * Razorpay webhook.
     *
     * This endpoint does NOT use JWT.
     *
     * Security is handled using
     * Razorpay webhook signature verification.
     */
    @PostMapping("/webhook")
    public ResponseEntity<WebhookResponseDto> processWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String webhookSignature) {

        paymentService.processRazorpayWebhook(payload, webhookSignature);

        return ResponseEntity.ok(new WebhookResponseDto("Webhook processed successfully"));
    }
}

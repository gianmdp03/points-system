package com.tech.point_system.service.impl;

import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.payment.PaymentStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/subscriptions")
@RequiredArgsConstructor
public class SubscriptionWebhookController {

    private final PaymentStrategyFactory paymentStrategyFactory;

    @PostMapping("/{provider}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable PaymentProvider provider,
            @RequestBody Map<String, Object> payload) {

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(provider);
        strategy.processWebhook(payload);

        return ResponseEntity.ok().build();
    }
}
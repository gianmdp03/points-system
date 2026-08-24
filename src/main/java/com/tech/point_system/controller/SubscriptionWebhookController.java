package com.tech.point_system.controller;

import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.payment.PaymentStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class SubscriptionWebhookController {

    private final PaymentStrategyFactory paymentStrategyFactory;

    @PostMapping("/subscriptions/{provider}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable("provider") PaymentProvider provider,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(required = false) Map<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> bodyPayload) {

        Map<String, Object> combinedPayload = new HashMap<>();
        if (queryParams != null) {
            combinedPayload.putAll(queryParams);
        }
        if (bodyPayload != null) {
            combinedPayload.putAll(bodyPayload);
        }

        log.info("[SUBSCRIPTION WEBHOOK] 📥 Petición recibida: Provider={}, xRequestId='{}', payload={}",
                provider, xRequestId, combinedPayload);

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(provider);
        CompletableFuture.runAsync(() -> {
            try {
                strategy.processWebhook(combinedPayload);
            } catch (Exception e) {
                log.error("[SUBSCRIPTION WEBHOOK] Error procesando webhook asíncrono para {}", provider, e);
            }
        });

        return ResponseEntity.ok().build();
    }
}


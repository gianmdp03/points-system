package com.tech.point_system.controller;

import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.payment.PaymentStrategyFactory;
import com.tech.point_system.service.mercadopago.MercadoPagoSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/subscriptions")
@RequiredArgsConstructor
public class SubscriptionWebhookController {

    private final PaymentStrategyFactory paymentStrategyFactory;
    private final MercadoPagoSignatureValidator signatureValidator;

    @PostMapping("/{provider}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable PaymentProvider provider,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
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

        if (provider == PaymentProvider.MERCADO_PAGO) {
            String dataId = extractDataId(combinedPayload);
            if (!signatureValidator.isValidSignature(xSignature, xRequestId, dataId)) {
                log.warn("[MERCADO PAGO WEBHOOK] Firma x-signature invalida o no autorizada");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(provider);
        CompletableFuture.runAsync(() -> {
            try {
                strategy.processWebhook(combinedPayload);
            } catch (Exception e) {
                log.error("[SUBSCRIPTION WEBHOOK] Error procesando webhook asincrono para {}", provider, e);
            }
        });

        return ResponseEntity.ok().build();
    }

    private String extractDataId(Map<String, Object> payload) {
        if (payload.containsKey("data") && payload.get("data") instanceof Map<?, ?> dataMap) {
            Object id = dataMap.get("id");
            if (id != null) return String.valueOf(id);
        }
        if (payload.containsKey("data.id")) {
            return String.valueOf(payload.get("data.id"));
        }
        if (payload.containsKey("id")) {
            return String.valueOf(payload.get("id"));
        }
        return null;
    }
}

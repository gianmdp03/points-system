package com.tech.point_system.payment.impl;

import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockPaymentStrategy implements PaymentStrategy {
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MOCK;
    }

    @Override
    public SubscriptionResponseDTO createSubscription(User user, SubscriptionRequestDTO dto) {
        log.info("[MOCK PAYMENT] Creando suscripción simulada para el usuario: {}", user.getEmail());

        String externalId = "MOCK-SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String checkoutUrl = "https://checkout.mock.local/pay/" + externalId;
        BigDecimal price = getMockPrice(dto.plan());
        String currency = "ARS";

        return new SubscriptionResponseDTO(
                null,
                dto.plan(),
                SubscriptionStatus.ACTIVE, // Simula aprobación inmediata en MVP
                getProvider(),
                price,
                currency,
                checkoutUrl,
                externalId
        );
    }

    @Override
    public void cancelSubscription(String externalSubscriptionId) {
        log.info("[MOCK PAYMENT] Cancelando suscripción MOCK externa: {}", externalSubscriptionId);
    }

    @Override
    public void processWebhook(Map<String, Object> payload) {
        log.info("[MOCK PAYMENT] Procesando webhook simulado. Payload: {}", payload);

        String externalId = (String) payload.get("externalSubscriptionId");
        String statusStr = (String) payload.get("status");

        if (externalId == null || statusStr == null) {
            log.warn("[MOCK PAYMENT] Webhook ignorado: faltan campos 'externalSubscriptionId' o 'status'");
            return;
        }

        Subscription subscription = subscriptionRepository.findByExternalSubscriptionId(externalId)
                .orElseThrow(() -> new NotFoundException("Suscripción no encontrada con id externo: " + externalId));

        subscription.setStatus(SubscriptionStatus.valueOf(statusStr.toUpperCase()));
        subscriptionRepository.save(subscription);

        log.info("[MOCK PAYMENT] Estado de suscripción {} actualizado a {} vía Webhook", externalId, statusStr);
    }

    private BigDecimal getMockPrice(SubscriptionPlan plan) {
        return switch (plan) {
            case BASIC -> new BigDecimal("9900.00");
            case PRO -> new BigDecimal("19900.00");
            case ENTERPRISE -> new BigDecimal("39900.00");
        };
    }
}
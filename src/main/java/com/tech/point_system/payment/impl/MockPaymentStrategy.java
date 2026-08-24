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
import com.tech.point_system.service.SubscriptionPlanConfigService;
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
    private final SubscriptionPlanConfigService planConfigService;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MOCK;
    }

    @Override
    public SubscriptionResponseDTO createSubscription(User user, SubscriptionRequestDTO dto) {
        log.info("[MOCK PAYMENT] Creando suscripcion simulada para el usuario: {}", user.getEmail());

        String externalId = "MOCK-SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String checkoutUrl = "https://checkout.mock.local/pay/" + externalId;
        BigDecimal price = planConfigService.getPlanPrice(dto.plan(), dto.billingPeriod(), "ARS");
        String currency = "ARS";

        return new SubscriptionResponseDTO(
                null,
                dto.plan(),
                SubscriptionStatus.ACTIVE,
                getProvider(),
                price,
                currency,
                checkoutUrl,
                externalId
        );
    }

    @Override
    public SubscriptionResponseDTO changeSubscriptionPlan(Subscription currentSubscription, SubscriptionPlan newPlan) {
        log.info("[MOCK PAYMENT] Procesando cambio de plan de suscripcion {} de {} a {}",
                currentSubscription.getExternalSubscriptionId(), currentSubscription.getPlan(), newPlan);

        BigDecimal price = planConfigService.getPlanPrice(newPlan, currentSubscription.getBillingPeriod(), "ARS");
        String currency = currentSubscription.getCurrency() != null ? currentSubscription.getCurrency() : "ARS";
        String externalId = currentSubscription.getExternalSubscriptionId() != null
                ? currentSubscription.getExternalSubscriptionId()
                : "MOCK-SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new SubscriptionResponseDTO(
                currentSubscription.getId(),
                newPlan,
                SubscriptionStatus.ACTIVE,
                getProvider(),
                price,
                currency,
                null,
                externalId
        );
    }

    @Override
    public SubscriptionResponseDTO upgradeSubscription(Subscription currentSubscription, SubscriptionPlan newPlan) {
        return changeSubscriptionPlan(currentSubscription, newPlan);
    }

    @Override
    public void cancelSubscription(String externalSubscriptionId) {
        log.info("[MOCK PAYMENT] Cancelando suscripcion MOCK externa: {}", externalSubscriptionId);
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
                .orElseThrow(() -> new NotFoundException("Suscripcion no encontrada con id externo: " + externalId));

        subscription.setStatus(SubscriptionStatus.valueOf(statusStr.toUpperCase()));
        subscriptionRepository.save(subscription);

        log.info("[MOCK PAYMENT] Estado de suscripcion {} actualizado a {} via Webhook", externalId, statusStr);
    }
}

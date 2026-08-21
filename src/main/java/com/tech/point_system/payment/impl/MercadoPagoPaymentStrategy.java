package com.tech.point_system.payment.impl;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;
import com.tech.point_system.config.MercadoPagoProperties;
import com.tech.point_system.dto.mercadopago.MercadoPagoAutoRecurring;
import com.tech.point_system.dto.mercadopago.MercadoPagoPreapprovalRequest;
import com.tech.point_system.dto.mercadopago.MercadoPagoPreapprovalResponse;
import com.tech.point_system.dto.mercadopago.MercadoPagoPreapprovalUpdateRequest;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.service.mercadopago.MercadoPagoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MercadoPagoPaymentStrategy implements PaymentStrategy {

    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoProperties mercadoPagoProperties;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MERCADO_PAGO;
    }

    @Override
    public SubscriptionResponseDTO createSubscription(User user, SubscriptionRequestDTO dto) {
        log.info("[MERCADO PAGO STRATEGY] Iniciando creación de suscripción para usuario: {}, Plan: {}", user.getEmail(), dto.plan());

        BigDecimal baseMonthlyPrice = getPlanPrice(dto.plan());
        int multiplier = dto.billingPeriod() == BillingPeriod.YEARLY ? 12 : 1;
        BigDecimal finalPrice = baseMonthlyPrice.multiply(BigDecimal.valueOf(multiplier));
        int frequency = dto.billingPeriod() == BillingPeriod.YEARLY ? 12 : 1;

        String returnUrl = (dto.returnUrl() != null && !dto.returnUrl().isBlank())
                ? dto.returnUrl()
                : mercadoPagoProperties.getBackUrl();

        String reason = "Pointly - Plan " + dto.plan().name() + " (" + (dto.billingPeriod() == BillingPeriod.YEARLY ? "Anual" : "Mensual") + ")";

        MercadoPagoAutoRecurring autoRecurring = new MercadoPagoAutoRecurring(
                frequency,
                "months",
                finalPrice,
                "ARS",
                null,
                null
        );

        MercadoPagoPreapprovalRequest request = new MercadoPagoPreapprovalRequest(
                reason,
                user.getEmail(),
                autoRecurring,
                returnUrl,
                user.getId(),
                "pending"
        );

        MercadoPagoPreapprovalResponse response = mercadoPagoClient.createPreapproval(request);

        log.info("[MERCADO PAGO STRATEGY] Suscripción creada en Mercado Pago con ID: {}, InitPoint: {}",
                response.id(), response.initPoint());

        return new SubscriptionResponseDTO(
                null,
                dto.plan(),
                mapStatus(response.status()),
                getProvider(),
                finalPrice,
                "ARS",
                response.initPoint(),
                response.id()
        );
    }

    @Override
    public SubscriptionResponseDTO changeSubscriptionPlan(Subscription currentSubscription, SubscriptionPlan newPlan) {
        log.info("[MERCADO PAGO STRATEGY] Cambiando plan de suscripción {} a {}",
                currentSubscription.getExternalSubscriptionId(), newPlan);

        BigDecimal baseMonthlyPrice = getPlanPrice(newPlan);
        int multiplier = currentSubscription.getBillingPeriod() == BillingPeriod.YEARLY ? 12 : 1;
        BigDecimal finalPrice = baseMonthlyPrice.multiply(BigDecimal.valueOf(multiplier));
        int frequency = currentSubscription.getBillingPeriod() == BillingPeriod.YEARLY ? 12 : 1;

        String externalId = currentSubscription.getExternalSubscriptionId();
        String reason = "Pointly - Plan " + newPlan.name() + " (" + (currentSubscription.getBillingPeriod() == BillingPeriod.YEARLY ? "Anual" : "Mensual") + ")";

        if (externalId != null && !externalId.isBlank()) {
            MercadoPagoAutoRecurring autoRecurring = new MercadoPagoAutoRecurring(
                    frequency,
                    "months",
                    finalPrice,
                    "ARS",
                    null,
                    null
            );

            MercadoPagoPreapprovalUpdateRequest updateRequest = new MercadoPagoPreapprovalUpdateRequest(
                    reason,
                    autoRecurring,
                    null,
                    null
            );

            mercadoPagoClient.updatePreapproval(externalId, updateRequest);
        }

        return new SubscriptionResponseDTO(
                currentSubscription.getId(),
                newPlan,
                currentSubscription.getStatus() != null ? currentSubscription.getStatus() : SubscriptionStatus.ACTIVE,
                getProvider(),
                finalPrice,
                "ARS",
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
        if (externalSubscriptionId == null || externalSubscriptionId.isBlank()) {
            log.warn("[MERCADO PAGO STRATEGY] No se puede cancelar suscripción sin externalSubscriptionId");
            return;
        }

        log.info("[MERCADO PAGO STRATEGY] Cancelando suscripción externa: {}", externalSubscriptionId);
        try {
            mercadoPagoClient.cancelPreapproval(externalSubscriptionId);
        } catch (Exception e) {
            log.error("[MERCADO PAGO STRATEGY] Error al cancelar suscripción {} en Mercado Pago", externalSubscriptionId, e);
        }
    }

    @Override
    public void processWebhook(Map<String, Object> payload) {
        log.info("[MERCADO PAGO STRATEGY] Procesando webhook de Mercado Pago. Payload: {}", payload);

        String preapprovalId = extractPreapprovalId(payload);
        if (preapprovalId == null || preapprovalId.isBlank()) {
            log.warn("[MERCADO PAGO STRATEGY] Webhook ignorado: no se encontró preapprovalId en el payload");
            return;
        }

        try {
            MercadoPagoPreapprovalResponse mpResponse = mercadoPagoClient.getPreapproval(preapprovalId);
            log.info("[MERCADO PAGO STRATEGY] Datos obtenidos de MP para suscripción {}: Status={}",
                    preapprovalId, mpResponse.status());

            subscriptionRepository.findByExternalSubscriptionId(preapprovalId).ifPresentOrElse(subscription -> {
                SubscriptionStatus newStatus = mapStatus(mpResponse.status());
                subscription.setStatus(newStatus);

                if (newStatus == SubscriptionStatus.CANCELLED) {
                    subscription.setCancelledAt(OffsetDateTime.now());
                }

                subscriptionRepository.save(subscription);
                log.info("[MERCADO PAGO STRATEGY] Suscripción {} actualizada a estado {}", preapprovalId, newStatus);
            }, () -> log.warn("[MERCADO PAGO STRATEGY] No se encontró suscripción local para externalId: {}", preapprovalId));

        } catch (Exception e) {
            log.error("[MERCADO PAGO STRATEGY] Error reconciliando estado de suscripción {}", preapprovalId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractPreapprovalId(Map<String, Object> payload) {
        if (payload.containsKey("data") && payload.get("data") instanceof Map<?, ?> dataMap) {
            Object id = dataMap.get("id");
            if (id != null) return String.valueOf(id);
        }
        if (payload.containsKey("id")) {
            return String.valueOf(payload.get("id"));
        }
        return null;
    }

    private BigDecimal getPlanPrice(SubscriptionPlan plan) {
        if (plan == null) return BigDecimal.ZERO;
        return switch (plan) {
            case NONE, FREE_TRIAL -> BigDecimal.ZERO;
            case BASIC -> new BigDecimal("9900.00");
            case PRO -> new BigDecimal("19900.00");
            case ENTERPRISE -> new BigDecimal("39900.00");
        };
    }

    public static SubscriptionStatus mapStatus(String mpStatus) {
        if (mpStatus == null) {
            return SubscriptionStatus.PENDING;
        }
        return switch (mpStatus.toLowerCase()) {
            case "authorized", "active" -> SubscriptionStatus.ACTIVE;
            case "pending" -> SubscriptionStatus.PENDING;
            case "paused" -> SubscriptionStatus.PAYMENT_FAILED;
            case "cancelled", "canceled" -> SubscriptionStatus.CANCELLED;
            case "rejected" -> SubscriptionStatus.PAYMENT_FAILED;
            default -> SubscriptionStatus.PENDING;
        };
    }
}

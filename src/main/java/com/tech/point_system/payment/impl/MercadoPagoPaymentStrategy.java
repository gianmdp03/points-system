package com.tech.point_system.payment.impl;

import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.payment.PaymentStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MercadoPagoPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MERCADO_PAGO;
    }

    @Override
    public SubscriptionResponseDTO createSubscription(User user, SubscriptionRequestDTO dto) {
        throw new UnsupportedOperationException("MercadoPago no está activo en el MVP. Utiliza MOCK.");
    }

    @Override
    public SubscriptionResponseDTO changeSubscriptionPlan(Subscription currentSubscription, SubscriptionPlan newPlan) {
        throw new UnsupportedOperationException("MercadoPago no está activo en el MVP. Utiliza MOCK.");
    }

    @Override
    public SubscriptionResponseDTO upgradeSubscription(Subscription currentSubscription, SubscriptionPlan newPlan) {
        return changeSubscriptionPlan(currentSubscription, newPlan);
    }

    @Override
    public void cancelSubscription(String externalSubscriptionId) {}

    @Override
    public void processWebhook(Map<String, Object> payload) {}
}

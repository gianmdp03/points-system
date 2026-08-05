package com.tech.point_system.payment;

import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.model.User;

import java.util.Map;

public interface PaymentStrategy {
    PaymentProvider getProvider();
    SubscriptionResponseDTO createSubscription(User user, SubscriptionRequestDTO dto);
    void cancelSubscription(String externalSubscriptionId);
    void processWebhook(Map<String, Object> payload);
}

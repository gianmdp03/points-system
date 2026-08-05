package com.tech.point_system.dto.subscription;

import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;

import java.math.BigDecimal;

public record SubscriptionResponseDTO(
        Long subscriptionId,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        PaymentProvider provider,
        BigDecimal price,
        String currency,
        String checkoutUrl,
        String externalSubscriptionId
) {}
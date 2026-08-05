package com.tech.point_system.dto.subscription;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SubscriptionDetailDTO(
        Long id,
        String userId,
        SubscriptionPlan plan,
        BillingPeriod billingPeriod,
        SubscriptionStatus status,
        PaymentProvider provider,
        BigDecimal price,
        String currency,
        String externalSubscriptionId,
        OffsetDateTime startDate,
        OffsetDateTime nextBillingDate,
        OffsetDateTime cancelledAt
) {}
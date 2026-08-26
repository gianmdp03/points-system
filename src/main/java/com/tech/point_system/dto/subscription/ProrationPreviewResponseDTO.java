package com.tech.point_system.dto.subscription;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.SubscriptionPlan;

import java.math.BigDecimal;

public record ProrationPreviewResponseDTO(
        SubscriptionPlan currentPlan,
        SubscriptionPlan newPlan,
        BillingPeriod billingPeriod,
        long totalDaysInPeriod,
        long remainingDays,
        BigDecimal currentPlanPrice,
        BigDecimal newPlanPrice,
        BigDecimal currentDailyRate,
        BigDecimal newDailyRate,
        BigDecimal proratedUpgradeAmount,
        String currency
) {}

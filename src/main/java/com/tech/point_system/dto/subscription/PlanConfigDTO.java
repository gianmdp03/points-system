package com.tech.point_system.dto.subscription;

import com.tech.point_system._enum.SubscriptionPlan;

import java.math.BigDecimal;
import java.util.List;

public record PlanConfigDTO(
        SubscriptionPlan plan,
        String name,
        String tagline,
        BigDecimal priceMonthlyArs,
        BigDecimal priceYearlyArs,
        BigDecimal priceMonthlyUsd,
        BigDecimal priceYearlyUsd,
        int maxClients,
        int maxRewards,
        int maxCompanies,
        boolean canCreatePromotions,
        boolean isPopular,
        List<String> features
) {}

package com.tech.point_system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "subscriptions")
public class SubscriptionPlanProperties {

    private Map<String, PlanItemProperties> plans = new HashMap<>();

    @Getter
    @Setter
    public static class PlanItemProperties {
        private String name = "";
        private String tagline = "";
        private BigDecimal priceMonthlyArs = BigDecimal.ZERO;
        private BigDecimal priceYearlyArs = BigDecimal.ZERO;
        private BigDecimal priceMonthlyUsd = BigDecimal.ZERO;
        private BigDecimal priceYearlyUsd = BigDecimal.ZERO;
        private int maxClients = 0;
        private int maxRewards = 0;
        private int maxCompanies = 1;
        private boolean canCreatePromotions = false;
        private boolean isPopular = false;
        private List<String> features = new ArrayList<>();
    }
}

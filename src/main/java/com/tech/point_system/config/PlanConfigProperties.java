package com.tech.point_system.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "plans")
public class PlanConfigProperties {
    private Map<String, PlanLimits> limits = new HashMap<>();

    public Map<String, PlanLimits> getLimits() {
        if (limits == null) {
            limits = new HashMap<>();
        }
        return limits;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanLimits {
        private int maxClients = 0;
        private int maxRewards = 0;
        private int maxCompanies = 0;
        private boolean canCreatePromotions = false;
    }
}

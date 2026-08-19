package com.tech.point_system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "plans")
public class PlanConfigProperties {
    private Map<String, PlanLimits> limits;

    @Getter
    @Setter
    public static class PlanLimits {
        private int maxClients;
        private int maxRewards;
        private int maxCompanies;
        private boolean canCreatePromotions;
    }
}
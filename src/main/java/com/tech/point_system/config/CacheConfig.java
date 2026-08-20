package com.tech.point_system.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PUBLIC_COMPANY_PRODUCTS = "public_company_products";
    public static final String PUBLIC_COMPANY_REWARDS = "public_company_rewards";
    public static final String COMPANY_ACTIVE_PROMOTIONS = "company_active_promotions";

    private ConcurrentMapCacheManager cacheManager;

    @Bean
    public CacheManager cacheManager() {
        this.cacheManager = new ConcurrentMapCacheManager(
                PUBLIC_COMPANY_PRODUCTS,
                PUBLIC_COMPANY_REWARDS,
                COMPANY_ACTIVE_PROMOTIONS
        );
        return this.cacheManager;
    }

    /**
     * TTL (Time-To-Live) de Seguridad - 30 Minutos:
     * Cada 30 minutos (1.800.000 ms) se purgan automáticamente las entradas en memoria para
     * refrescar catálogos estáticos, complementando la invalidación instantánea de @CacheEvict.
     */
    @Scheduled(fixedRate = 1800000) // 30 minutos
    public void evictAllCachesAtIntervals() {
        if (cacheManager != null) {
            for (String name : cacheManager.getCacheNames()) {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            }
        }
    }
}

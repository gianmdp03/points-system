package com.tech.point_system.service;

import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.config.PlanConfigProperties;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanValidatorService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanConfigProperties planConfig;

    private SubscriptionPlan getActivePlan(String companyAdminId) {
        return subscriptionRepository.findByUserId(companyAdminId)
                .map(Subscription::getPlan)
                .orElseThrow(() -> new NotFoundException("No se encontró una suscripción activa."));
    }

    private PlanConfigProperties.PlanLimits getLimitsForPlan(SubscriptionPlan plan) {
        PlanConfigProperties.PlanLimits limits = planConfig.getLimits().get(plan.name().toLowerCase());
        if (limits == null) {
            throw new IllegalStateException("Configuración no encontrada en properties para el plan: " + plan.name());
        }
        return limits;
    }

    public void validateClientCreation(String companyAdminId, int currentClientCount) {
        SubscriptionPlan plan = getActivePlan(companyAdminId);
        PlanConfigProperties.PlanLimits limits = getLimitsForPlan(plan);

        if (limits.getMaxClients() != -1 && currentClientCount >= limits.getMaxClients()) {
            throw new ConflictException("Has alcanzado el límite de clientes registrados para tu plan " + plan.name() + ". Actualiza a un plan superior.");
        }
    }

    public void validateRewardCreation(String companyAdminId, int currentRewardCount) {
        SubscriptionPlan plan = getActivePlan(companyAdminId);
        PlanConfigProperties.PlanLimits limits = getLimitsForPlan(plan);

        if (limits.getMaxRewards() != -1 && currentRewardCount >= limits.getMaxRewards()) {
            throw new ConflictException("Has alcanzado el límite de premios para tu plan " + plan.name() + ". Actualiza a un plan superior.");
        }
    }

    public void validateCompanyCreation(String companyAdminId, int currentCompanyCount) {
        SubscriptionPlan plan = getActivePlan(companyAdminId);
        PlanConfigProperties.PlanLimits limits = getLimitsForPlan(plan);

        if (limits.getMaxCompanies() != -1 && currentCompanyCount >= limits.getMaxCompanies()) {
            throw new ConflictException("Has alcanzado el límite de sucursales/empresas para tu plan " + plan.name() + ". Actualiza a un plan superior.");
        }
    }

    public void validatePromotionCreation(String companyAdminId) {
        SubscriptionPlan plan = getActivePlan(companyAdminId);
        PlanConfigProperties.PlanLimits limits = getLimitsForPlan(plan);

        if (!limits.isCanCreatePromotions()) {
            throw new ConflictException("Tu plan " + plan.name() + " no permite crear promociones. Actualiza a un plan superior.");
        }
    }
}
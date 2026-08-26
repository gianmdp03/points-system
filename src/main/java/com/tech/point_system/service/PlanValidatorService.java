package com.tech.point_system.service;

import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.config.PlanConfigProperties;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlanValidatorService {

    private final PlanConfigProperties planConfigProperties;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PointsAccountRepository pointsAccountRepository;
    private final RewardRepository rewardRepository;
    private final PromotionRepository promotionRepository;

    public SubscriptionPlan getActivePlan(String companyAdminId) {
        User user = userRepository.findById(companyAdminId).orElse(null);
        if (user != null) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            if (user.getCurrentPlan() != null && user.getCurrentPlan() != SubscriptionPlan.NONE) {
                if (user.getPlanExpirationDate() == null || user.getPlanExpirationDate().isAfter(now)) {
                    return user.getCurrentPlan();
                }
            }

            if (Boolean.FALSE.equals(user.getIsFreeTrialOver())) {
                return SubscriptionPlan.FREE_TRIAL;
            }
        }

        return SubscriptionPlan.NONE;
    }

    public PlanConfigProperties.PlanLimits getLimitsForPlan(SubscriptionPlan plan) {
        if (plan == null || plan == SubscriptionPlan.NONE) {
            return new PlanConfigProperties.PlanLimits(0, 0, 0, false);
        }
        String key = plan.name().toLowerCase();
        if (plan == SubscriptionPlan.FREE_TRIAL) {
            key = "pro";
        }
        return planConfigProperties.getLimits().getOrDefault(key, new PlanConfigProperties.PlanLimits(0, 0, 0, false));
    }

    public void validatePlanChangeEligibility(String companyAdminId, SubscriptionPlan targetPlan) {
        PlanConfigProperties.PlanLimits limits = getLimitsForPlan(targetPlan);
        List<Company> companies = companyRepository.findAllByAdminId(companyAdminId);

        // 1. Validar sucursales (O(1))
        if (limits.getMaxCompanies() != -1 && companies.size() > limits.getMaxCompanies()) {
            throw new ConflictException("No puedes cambiar al plan " + targetPlan.name() +
                    " porque tienes " + companies.size() + " sucursales registradas y el límite permitido es " + limits.getMaxCompanies() +
                    ". Por favor, desactiva o elimina sucursales antes de cambiar de plan.");
        }

        // 2. Iterar por cada sucursal y validar clientes, premios y promociones usando conteos directos O(1)
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (Company company : companies) {
            // Clientes (COUNT O(1))
            long clientCount = pointsAccountRepository.countByCompanyId(company.getId());
            if (limits.getMaxClients() != -1 && clientCount > limits.getMaxClients()) {
                throw new ConflictException("No puedes cambiar al plan " + targetPlan.name() +
                        " porque la sucursal '" + company.getName() + "' tiene " + clientCount +
                        " clientes registrados y el límite permitido es " + limits.getMaxClients() + ". Elimina clientes sobrantes antes de continuar.");
            }

            // Premios (COUNT O(1) - sin cargar entidades en memoria)
            long rewardCount = rewardRepository.countByCompanyId(company.getId());
            if (limits.getMaxRewards() != -1 && rewardCount > limits.getMaxRewards()) {
                throw new ConflictException("No puedes cambiar al plan " + targetPlan.name() +
                        " porque la sucursal '" + company.getName() + "' tiene " + rewardCount +
                        " premios registrados y el límite permitido es " + limits.getMaxRewards() +
                        ". Elimina premios sobrantes antes de continuar.");
            }

            // Promociones (EXISTS BOOLEAN O(1))
            if (!limits.isCanCreatePromotions()) {
                boolean hasActivePromos = promotionRepository.existsActivePromotion(company.getId(), now);
                if (hasActivePromos) {
                    throw new ConflictException("No puedes cambiar al plan " + targetPlan.name() +
                            " porque la sucursal '" + company.getName() + "' tiene promociones activas y este plan no las incluye. " +
                            "Por favor, desactiva tus promociones vigentes primero.");
                }
            }
        }
    }

    public void validateClientCreation(String companyAdminId, int currentClientCount) {
        SubscriptionPlan plan = getActivePlan(companyAdminId);
        if (plan == SubscriptionPlan.NONE) {
            throw new ConflictException("No posees un plan de suscripcion activo ni periodo de prueba. Por favor, selecciona un plan para registrar clientes.");
        }

        PlanConfigProperties.PlanLimits limits = getLimitsForPlan(plan);
        if (limits.getMaxClients() != -1 && currentClientCount >= limits.getMaxClients()) {
            throw new ConflictException("Has alcanzado el limite de clientes registrados para tu plan " + plan.name() + ". Actualiza a un plan superior.");
        }
    }

    public void validateRewardCreation(String companyAdminId, int currentRewardCount) {
        SubscriptionPlan plan = getActivePlan(companyAdminId);
        if (plan == SubscriptionPlan.NONE) {
            throw new ConflictException("No posees un plan de suscripcion activo ni periodo de prueba. Por favor, selecciona un plan para crear premios.");
        }

        PlanConfigProperties.PlanLimits limits = getLimitsForPlan(plan);
        if (limits.getMaxRewards() != -1 && currentRewardCount >= limits.getMaxRewards()) {
            throw new ConflictException("Has alcanzado el limite de premios para tu plan " + plan.name() + ". Actualiza a un plan superior.");
        }
    }

    public void validateCompanyCreation(String companyAdminId, int currentCompanyCount) {
        SubscriptionPlan plan = getActivePlan(companyAdminId);
        if (plan == SubscriptionPlan.NONE) {
            throw new ConflictException("No posees un plan de suscripcion activo ni periodo de prueba. Por favor, selecciona un plan para crear y administrar empresas.");
        }

        PlanConfigProperties.PlanLimits limits = getLimitsForPlan(plan);
        if (limits.getMaxCompanies() != -1 && currentCompanyCount >= limits.getMaxCompanies()) {
            throw new ConflictException("Has alcanzado el limite de sucursales/empresas para tu plan " + plan.name() + ". Actualiza a un plan superior.");
        }
    }

    public void validatePromotionCreation(String companyAdminId) {
        SubscriptionPlan plan = getActivePlan(companyAdminId);
        if (plan == SubscriptionPlan.NONE) {
            throw new ConflictException("No posees un plan de suscripcion activo ni periodo de prueba. Por favor, selecciona un plan para crear promociones.");
        }

        PlanConfigProperties.PlanLimits limits = getLimitsForPlan(plan);
        if (!limits.isCanCreatePromotions()) {
            throw new ConflictException("Tu plan " + plan.name() + " no permite crear promociones. Actualiza a un plan superior.");
        }
    }
}

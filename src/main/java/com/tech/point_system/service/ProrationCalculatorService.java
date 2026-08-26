package com.tech.point_system.service;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.dto.subscription.ProrationPreviewResponseDTO;
import com.tech.point_system.exception.BadRequestException;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProrationCalculatorService {

    private final SubscriptionPlanConfigService planConfigService;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    /**
     * Calcula el desglose prorrateado para un Upgrade de plan sobre un usuario y su orden de suscripción.
     * Formula: Monto = (Tarifa Diaria Nuevo Plan - Tarifa Diaria Plan Actual) * Dias Restantes
     */
    public ProrationPreviewResponseDTO calculateUpgradeProration(Subscription subscription, SubscriptionPlan newPlan) {
        if (subscription == null || subscription.getUser() == null) {
            throw new BadRequestException("La suscripción y el usuario no pueden ser nulos para calcular el prorrateo.");
        }

        User user = subscription.getUser();
        return calculateUpgradeProrationForUser(user, subscription, newPlan);
    }

    public ProrationPreviewResponseDTO calculateUpgradeProrationForUser(User user, Subscription subscription, SubscriptionPlan newPlan) {
        if (user == null) {
            throw new BadRequestException("El usuario no puede ser nulo para calcular el prorrateo.");
        }
        if (newPlan == null) {
            throw new BadRequestException("Debes seleccionar un plan comercial válido (BASIC, PRO o ENTERPRISE) para el Upgrade.");
        }

        SubscriptionPlan currentPlan = user.getCurrentPlan() != null ? user.getCurrentPlan() : SubscriptionPlan.NONE;
        if (currentPlan == SubscriptionPlan.NONE && Boolean.FALSE.equals(user.getIsFreeTrialOver())) {
            currentPlan = SubscriptionPlan.FREE_TRIAL;
        }

        if (currentPlan == newPlan) {
            throw new BadRequestException("El nuevo plan debe ser diferente al plan actual (" + currentPlan + ").");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long remainingDays = 0;
        if (user.getPlanExpirationDate() != null && user.getPlanExpirationDate().isAfter(now)) {
            remainingDays = com.tech.point_system.service.impl.SubscriptionServiceImpl.calculateDaysRemaining(now, user.getPlanExpirationDate());
        }

        int currentTier = SubscriptionPlan.getTierOf(currentPlan);
        int targetTier = SubscriptionPlan.getTierOf(newPlan);

        // Anti-Downgrade: Bloquear downgrade si tiene saldo a favor
        if (targetTier < currentTier) {
            if (remainingDays > 0) {
                throw new ConflictException(
                        "No puedes realizar un downgrade de plan teniendo días de cobertura activos a favor. Debes esperar a que caduque tu periodo actual."
                );
            }
            throw new BadRequestException("El plan " + newPlan + " no representa un Upgrade respecto a " + currentPlan + ".");
        }

        if (newPlan == SubscriptionPlan.NONE || newPlan == SubscriptionPlan.FREE_TRIAL) {
            throw new BadRequestException("Debes seleccionar un plan comercial válido (BASIC, PRO o ENTERPRISE) para el Upgrade.");
        }

        BillingPeriod billingPeriod = subscription != null && subscription.getBillingPeriod() != null
                ? subscription.getBillingPeriod()
                : BillingPeriod.MONTHLY;

        String currency = subscription != null && subscription.getCurrency() != null
                ? subscription.getCurrency()
                : "ARS";

        long totalDaysInPeriod = billingPeriod.getDays() > 0 ? billingPeriod.getDays() : 30;

        // Precios de catálogo
        BigDecimal currentPlanPrice = planConfigService.getPlanPrice(currentPlan, billingPeriod, currency);
        BigDecimal newPlanPrice = planConfigService.getPlanPrice(newPlan, billingPeriod, currency);

        // Tarifas diarias
        BigDecimal currentDailyRate = currentPlanPrice.divide(BigDecimal.valueOf(totalDaysInPeriod), 4, RoundingMode.HALF_UP);
        BigDecimal newDailyRate = newPlanPrice.divide(BigDecimal.valueOf(totalDaysInPeriod), 4, RoundingMode.HALF_UP);

        // Diferencial diario
        BigDecimal dailyRateDiff = newDailyRate.subtract(currentDailyRate);
        if (dailyRateDiff.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException(
                    "No puedes realizar un downgrade de plan teniendo días de cobertura activos a favor. Debes esperar a que caduque tu periodo actual."
            );
        }

        // Monto prorrateado
        BigDecimal proratedUpgradeAmount;
        if (remainingDays > 0) {
            proratedUpgradeAmount = dailyRateDiff.multiply(BigDecimal.valueOf(remainingDays))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            proratedUpgradeAmount = newPlanPrice;
        }

        log.info("[PRORATION] Upgrade de {} a {}: Total días={}, Días restantes={}, Tarifa actual/día={}, Tarifa nueva/día={}, Monto a cobrar={}",
                currentPlan, newPlan, totalDaysInPeriod, remainingDays, currentDailyRate, newDailyRate, proratedUpgradeAmount);

        return new ProrationPreviewResponseDTO(
                currentPlan,
                newPlan,
                billingPeriod,
                totalDaysInPeriod,
                remainingDays,
                currentPlanPrice,
                newPlanPrice,
                currentDailyRate,
                newDailyRate,
                proratedUpgradeAmount,
                currency
        );
    }

    /**
     * Endpoint preview para que el frontend obtenga el cálculo antes de renderizar el checkout de Upgrade.
     */
    public ProrationPreviewResponseDTO previewUpgrade(String userId, SubscriptionPlan newPlan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("No se encontró usuario con ID: " + userId));

        Subscription subscription = subscriptionRepository.findByUserId(userId).orElse(null);

        return calculateUpgradeProrationForUser(user, subscription, newPlan);
    }
}


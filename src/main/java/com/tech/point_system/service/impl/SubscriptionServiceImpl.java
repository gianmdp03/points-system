package com.tech.point_system.service.impl;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;
import com.tech.point_system.dto.subscription.SubscriptionDetailDTO;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.dto.subscription.SubscriptionUpgradeRequestDTO;
import com.tech.point_system.exception.BadRequestException;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.payment.PaymentStrategyFactory;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.repository.UserRepository;
import com.tech.point_system.service.PlanValidatorService;
import com.tech.point_system.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final PlanValidatorService planValidatorService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubscriptionResponseDTO subscribeCompanyAdmin(String userId, SubscriptionRequestDTO dto) {
        log.info("[SUBSCRIPTION SERVICE] 🚀 [SUBSCRIBE/EXTEND] Solicitud de suscripción para usuario '{}' | Plan='{}' | Periodo='{}' | Provider='{}'",
                userId, dto.plan(), dto.billingPeriod(), dto.provider());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + userId));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        boolean hasActivePlan = user.getCurrentPlan() != null
                && user.getCurrentPlan() != SubscriptionPlan.NONE
                && user.getPlanExpirationDate() != null
                && user.getPlanExpirationDate().isAfter(now);

        boolean isDebtRegularization = (user.getPendingDebtArs() != null && user.getPendingDebtArs().compareTo(BigDecimal.ZERO) > 0)
                || Boolean.TRUE.equals(user.getIsSuspendedForChargeback());

        if (hasActivePlan && !isDebtRegularization) {
            SubscriptionPlan activePlan = user.getCurrentPlan();
            if (activePlan != dto.plan()) {
                int activeTier = SubscriptionPlan.getTierOf(activePlan);
                int targetTier = SubscriptionPlan.getTierOf(dto.plan());

                if (targetTier > activeTier) {
                    log.warn("[SUBSCRIPTION SERVICE] ⚠️ Usuario '{}' tiene un plan activo '{}' diferente al solicitado '{}'. Debe usar upgrade.",
                            userId, activePlan, dto.plan());
                    throw new BadRequestException("Ya posees el plan " + activePlan + " activo. Para cambiar a otro plan utiliza la opción de Upgrade.");
                } else {
                    log.warn("[SUBSCRIPTION SERVICE] ⚠️ Usuario '{}' intentó downgrade a '{}' con plan '{}' vigente hasta {}",
                            userId, dto.plan(), activePlan, user.getPlanExpirationDate());
                    throw new ConflictException("No puedes cambiar a un plan inferior mientras tengas días de cobertura activos a favor. Debes esperar a que caduque tu periodo actual.");
                }
            }
            log.info("[SUBSCRIPTION SERVICE] ➕ Usuario '{}' está extendiendo/recargando días para su plan actual '{}' (Vencimiento actual: {})",
                    userId, activePlan, user.getPlanExpirationDate());
        }

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(dto.provider());
        log.info("[SUBSCRIPTION SERVICE] 💳 Invocando estrategia de pago: {}", strategy.getClass().getSimpleName());

        SubscriptionResponseDTO gatewayResponse = strategy.createSubscription(user, dto);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(dto.plan())
                .billingPeriod(dto.billingPeriod())
                .status(SubscriptionStatus.PENDING) // Zero-Trust: Siempre PENDING hasta recibir webhook de pago
                .provider(dto.provider())
                .price(gatewayResponse.price())
                .currency(gatewayResponse.currency())
                .externalSubscriptionId(gatewayResponse.externalSubscriptionId())
                .startDate(now)
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.info("[SUBSCRIPTION SERVICE] ✅ Orden de Suscripción PENDING registrada en DB: ID={} | Plan='{}' | ExternalRef='{}'",
                subscription.getId(), subscription.getPlan(), subscription.getExternalSubscriptionId());

        return new SubscriptionResponseDTO(
                subscription.getId(),
                subscription.getPlan(),
                SubscriptionStatus.PENDING,
                subscription.getProvider(),
                subscription.getPrice(),
                subscription.getCurrency(),
                gatewayResponse.checkoutUrl(),
                subscription.getExternalSubscriptionId()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubscriptionResponseDTO upgradeSubscription(String userId, SubscriptionPlan newPlan) {
        return upgradeSubscription(userId, new SubscriptionUpgradeRequestDTO(newPlan));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubscriptionResponseDTO upgradeSubscription(String userId, SubscriptionUpgradeRequestDTO dto) {
        SubscriptionPlan newPlan = dto.newPlan();
        log.info("[SUBSCRIPTION SERVICE] 🚀 [UPGRADE] Solicitud de upgrade para usuario '{}' hacia plan '{}'", userId, newPlan);

        if (newPlan == null || newPlan == SubscriptionPlan.NONE || newPlan == SubscriptionPlan.FREE_TRIAL) {
            log.warn("[SUBSCRIPTION SERVICE] ⚠️ Plan inválido seleccionado para upgrade: '{}'", newPlan);
            throw new ConflictException("Debes seleccionar un plan comercial valido (BASIC, PRO o ENTERPRISE).");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        SubscriptionPlan effectiveCurrentPlan = (user.getCurrentPlan() != null && user.getCurrentPlan() != SubscriptionPlan.NONE)
                ? user.getCurrentPlan()
                : (Boolean.FALSE.equals(user.getIsFreeTrialOver()) ? SubscriptionPlan.FREE_TRIAL : SubscriptionPlan.NONE);

        if (effectiveCurrentPlan == newPlan) {
            log.warn("[SUBSCRIPTION SERVICE] ⚠️ El usuario ya cuenta con el plan '{}'", newPlan);
            throw new ConflictException("Ya tienes contratado el plan " + newPlan);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int currentTier = SubscriptionPlan.getTierOf(effectiveCurrentPlan);
        int targetTier = SubscriptionPlan.getTierOf(newPlan);

        if (targetTier < currentTier) {
            if (user.getPlanExpirationDate() != null && user.getPlanExpirationDate().isAfter(now)) {
                log.warn("[SUBSCRIPTION SERVICE] ⛔ Downgrade bloqueado para usuario '{}'. Días activos hasta {}", userId, user.getPlanExpirationDate());
                throw new ConflictException("No puedes realizar un downgrade de plan teniendo días de cobertura activos a favor. Debes esperar a que caduque tu periodo actual.");
            }
        }

        // Validar que los recursos actuales no superen los limites del nuevo plan (Anti-Downgrade Loophole)
        planValidatorService.validatePlanChangeEligibility(userId, newPlan);

        Optional<Subscription> subOpt = subscriptionRepository.findTopByUserIdOrderByIdDesc(userId);
        Subscription currentSubscription = subOpt.orElseGet(() -> Subscription.builder()
                .user(user)
                .plan(effectiveCurrentPlan)
                .billingPeriod(BillingPeriod.MONTHLY)
                .status(SubscriptionStatus.APPROVED)
                .provider(PaymentProvider.MERCADO_PAGO)
                .price(BigDecimal.ZERO)
                .currency("ARS")
                .startDate(now)
                .build());

        PaymentProvider provider = currentSubscription.getProvider() != null ? currentSubscription.getProvider() : PaymentProvider.MERCADO_PAGO;
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(provider);
        log.info("[SUBSCRIPTION SERVICE] 💳 Invocando upgrade en estrategia de pago '{}'...", strategy.getClass().getSimpleName());

        // Zero-Trust: Generar preferencia de pago. NO mutar el plan ni activar la suscripción en DB hasta confirmación del webhook.
        SubscriptionResponseDTO changeResponse = strategy.upgradeSubscription(currentSubscription, newPlan, dto);

        log.info("[SUBSCRIPTION SERVICE] 🔗 Preferencia de Upgrade generada con éxito para usuario '{}' -> CheckoutUrl='{}' | ExternalRef='{}'",
                user.getEmail(), changeResponse.checkoutUrl(), changeResponse.externalSubscriptionId());

        return new SubscriptionResponseDTO(
                currentSubscription.getId(),
                newPlan,
                SubscriptionStatus.PENDING,
                provider,
                changeResponse.price(),
                changeResponse.currency(),
                changeResponse.checkoutUrl(),
                changeResponse.externalSubscriptionId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDetailDTO getMySubscription(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Optional<Subscription> subOpt = subscriptionRepository.findTopByUserIdOrderByIdDesc(userId);
        Subscription latestSub = subOpt.orElse(null);

        boolean hasCommercialPlan = user.getCurrentPlan() != null
                && user.getCurrentPlan() != SubscriptionPlan.NONE
                && user.getCurrentPlan() != SubscriptionPlan.FREE_TRIAL
                && user.getPlanExpirationDate() != null
                && user.getPlanExpirationDate().isAfter(now);

        if (hasCommercialPlan) {
            long daysRemaining = calculateDaysRemaining(now, user.getPlanExpirationDate());
            log.info("[SUBSCRIPTION SERVICE] 🔍 Devolviendo plan activo '{}' para usuario '{}' (Vence: {}, Días={})",
                    user.getCurrentPlan(), userId, user.getPlanExpirationDate(), daysRemaining);

            return new SubscriptionDetailDTO(
                    latestSub != null ? latestSub.getId() : null,
                    userId,
                    user.getCurrentPlan(),
                    latestSub != null ? latestSub.getBillingPeriod() : BillingPeriod.MONTHLY,
                    SubscriptionStatus.APPROVED,
                    latestSub != null ? latestSub.getProvider() : PaymentProvider.MERCADO_PAGO,
                    latestSub != null ? latestSub.getPrice() : BigDecimal.ZERO,
                    latestSub != null ? latestSub.getCurrency() : "ARS",
                    latestSub != null ? latestSub.getExternalSubscriptionId() : null,
                    latestSub != null ? latestSub.getStartDate() : now,
                    user.getPlanExpirationDate(),
                    daysRemaining
            );
        }

        if (Boolean.FALSE.equals(user.getIsFreeTrialOver())) {
            OffsetDateTime start = user.getFreeTrialStartTime() != null
                    ? user.getFreeTrialStartTime().atStartOfDay().atOffset(ZoneOffset.UTC)
                    : now;
            OffsetDateTime end = user.getPlanExpirationDate() != null
                    ? user.getPlanExpirationDate()
                    : (user.getFreeTrialEndTime() != null
                    ? user.getFreeTrialEndTime().atStartOfDay().atOffset(ZoneOffset.UTC)
                    : start.plusDays(30));

            long daysRemaining = calculateDaysRemaining(now, end);

            log.info("[SUBSCRIPTION SERVICE] 🔍 Devolviendo estado de FREE_TRIAL activo para usuario '{}' (Vence: {}, Días={})", userId, end, daysRemaining);

            return new SubscriptionDetailDTO(
                    latestSub != null ? latestSub.getId() : null,
                    userId,
                    SubscriptionPlan.FREE_TRIAL,
                    BillingPeriod.MONTHLY,
                    SubscriptionStatus.APPROVED,
                    PaymentProvider.MOCK,
                    BigDecimal.ZERO,
                    "ARS",
                    "FREE-TRIAL-" + userId,
                    start,
                    end,
                    daysRemaining
            );
        }

        // Si hay una orden PENDING en curso, reportarla con status PENDING
        if (latestSub != null && latestSub.getStatus() == SubscriptionStatus.PENDING) {
            return new SubscriptionDetailDTO(
                    latestSub.getId(),
                    userId,
                    latestSub.getPlan(),
                    latestSub.getBillingPeriod(),
                    SubscriptionStatus.PENDING,
                    latestSub.getProvider(),
                    latestSub.getPrice(),
                    latestSub.getCurrency(),
                    latestSub.getExternalSubscriptionId(),
                    latestSub.getStartDate(),
                    user.getPlanExpirationDate(),
                    0L
            );
        }

        log.info("[SUBSCRIPTION SERVICE] 🔍 Usuario '{}' no posee suscripción activa ni periodo de prueba. Devolviendo plan NONE.", userId);

        return new SubscriptionDetailDTO(
                null,
                userId,
                SubscriptionPlan.NONE,
                BillingPeriod.MONTHLY,
                SubscriptionStatus.EXPIRED,
                PaymentProvider.MOCK,
                BigDecimal.ZERO,
                "ARS",
                null,
                null,
                null,
                0L
        );
    }

    public static long calculateDaysRemaining(OffsetDateTime now, OffsetDateTime expirationDate) {
        if (expirationDate == null || !expirationDate.isAfter(now)) {
            return 0L;
        }
        long secondsRemaining = java.time.Duration.between(now, expirationDate).toSeconds();
        if (secondsRemaining <= 0) {
            return 0L;
        }
        return (secondsRemaining + 86399L) / 86400L;
    }
}



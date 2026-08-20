package com.tech.point_system.service.impl;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;
import com.tech.point_system.dto.subscription.SubscriptionDetailDTO;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.repository.UserRepository;
import com.tech.point_system.service.PlanValidatorService;
import com.tech.point_system.service.SubscriptionService;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.payment.PaymentStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final PlanValidatorService planValidatorService;

    @Override
    @Transactional
    public SubscriptionResponseDTO subscribeCompanyAdmin(String userId, SubscriptionRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        subscriptionRepository.findByUserId(userId).ifPresent(existingSub -> {
            if (existingSub.getStatus() == SubscriptionStatus.ACTIVE) {
                throw new ConflictException("Ya tienes una suscripcion activa.");
            }
        });

        Company company = null;
        if (dto.companyId() != null) {
            company = companyRepository.findById(dto.companyId())
                    .orElseThrow(() -> new NotFoundException("Empresa no encontrada"));
        }

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(dto.provider());
        SubscriptionResponseDTO gatewayResponse = strategy.createSubscription(user, dto);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime nextBilling = dto.billingPeriod() == BillingPeriod.YEARLY
                ? now.plusYears(1)
                : now.plusMonths(1);

        Subscription subscription = Subscription.builder()
                .user(user)
                .company(company)
                .plan(dto.plan())
                .billingPeriod(dto.billingPeriod())
                .status(gatewayResponse.status())
                .provider(dto.provider())
                .price(gatewayResponse.price())
                .currency(gatewayResponse.currency())
                .externalSubscriptionId(gatewayResponse.externalSubscriptionId())
                .startDate(now)
                .nextBillingDate(nextBilling)
                .build();

        subscription = subscriptionRepository.save(subscription);

        return new SubscriptionResponseDTO(
                subscription.getId(),
                subscription.getPlan(),
                subscription.getStatus(),
                subscription.getProvider(),
                subscription.getPrice(),
                subscription.getCurrency(),
                gatewayResponse.checkoutUrl(),
                subscription.getExternalSubscriptionId()
        );
    }

    @Override
    @Transactional
    public SubscriptionDetailDTO changeSubscriptionPlan(String userId, SubscriptionPlan newPlan) {
        if (newPlan == null || newPlan == SubscriptionPlan.NONE || newPlan == SubscriptionPlan.FREE_TRIAL) {
            throw new ConflictException("Debes seleccionar un plan comercial valido (BASIC, PRO o ENTERPRISE).");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Optional<Subscription> subOpt = subscriptionRepository.findByUserId(userId);
        Subscription currentSubscription;

        if (subOpt.isPresent()) {
            currentSubscription = subOpt.get();
            if (currentSubscription.getStatus() != SubscriptionStatus.ACTIVE) {
                currentSubscription.setStatus(SubscriptionStatus.ACTIVE);
            }
            if (currentSubscription.getPlan() == newPlan) {
                throw new ConflictException("Ya tienes contratado el plan " + newPlan);
            }
        } else {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            currentSubscription = Subscription.builder()
                    .user(user)
                    .plan(SubscriptionPlan.FREE_TRIAL)
                    .billingPeriod(BillingPeriod.MONTHLY)
                    .status(SubscriptionStatus.ACTIVE)
                    .provider(PaymentProvider.MOCK)
                    .price(BigDecimal.ZERO)
                    .currency("ARS")
                    .startDate(now)
                    .nextBillingDate(now.plusMonths(1))
                    .build();
        }

        // Validar que los recursos actuales no superen los limites del nuevo plan (Anti-Downgrade Loophole)
        planValidatorService.validatePlanChangeEligibility(userId, newPlan);

        PaymentProvider provider = currentSubscription.getProvider() != null ? currentSubscription.getProvider() : PaymentProvider.MOCK;
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(provider);
        SubscriptionResponseDTO changeResponse = strategy.changeSubscriptionPlan(currentSubscription, newPlan);

        currentSubscription.setPlan(newPlan);
        currentSubscription.setPrice(changeResponse.price());
        currentSubscription.setCurrency(changeResponse.currency());
        currentSubscription.setStatus(SubscriptionStatus.ACTIVE);
        if (changeResponse.externalSubscriptionId() != null) {
            currentSubscription.setExternalSubscriptionId(changeResponse.externalSubscriptionId());
        }

        user.setIsFreeTrialOver(true);
        userRepository.save(user);

        currentSubscription = subscriptionRepository.save(currentSubscription);

        log.info("[SUBSCRIPTION PLAN CHANGE] Usuario {} cambio exitosamente a plan {}", user.getEmail(), newPlan);

        return new SubscriptionDetailDTO(
                currentSubscription.getId(),
                user.getId(),
                currentSubscription.getPlan(),
                currentSubscription.getBillingPeriod(),
                currentSubscription.getStatus(),
                currentSubscription.getProvider(),
                currentSubscription.getPrice(),
                currentSubscription.getCurrency(),
                currentSubscription.getExternalSubscriptionId(),
                currentSubscription.getStartDate(),
                currentSubscription.getNextBillingDate(),
                currentSubscription.getCancelledAt()
        );
    }

    @Override
    @Transactional
    public SubscriptionDetailDTO upgradeSubscription(String userId, SubscriptionPlan newPlan) {
        return changeSubscriptionPlan(userId, newPlan);
    }

    @Override
    public SubscriptionDetailDTO getMySubscription(String userId) {
        Optional<Subscription> subOpt = subscriptionRepository.findByUserId(userId);
        if (subOpt.isPresent() && subOpt.get().getStatus() == SubscriptionStatus.ACTIVE) {
            Subscription subscription = subOpt.get();
            return new SubscriptionDetailDTO(
                    subscription.getId(),
                    subscription.getUser().getId(),
                    subscription.getPlan(),
                    subscription.getBillingPeriod(),
                    subscription.getStatus(),
                    subscription.getProvider(),
                    subscription.getPrice(),
                    subscription.getCurrency(),
                    subscription.getExternalSubscriptionId(),
                    subscription.getStartDate(),
                    subscription.getNextBillingDate(),
                    subscription.getCancelledAt()
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (Boolean.FALSE.equals(user.getIsFreeTrialOver())) {
            OffsetDateTime start = user.getFreeTrialStartTime() != null
                    ? user.getFreeTrialStartTime().atStartOfDay().atOffset(ZoneOffset.UTC)
                    : OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime end = user.getFreeTrialEndTime() != null
                    ? user.getFreeTrialEndTime().atStartOfDay().atOffset(ZoneOffset.UTC)
                    : start.plusDays(30);

            return new SubscriptionDetailDTO(
                    null,
                    userId,
                    SubscriptionPlan.FREE_TRIAL,
                    BillingPeriod.MONTHLY,
                    SubscriptionStatus.ACTIVE,
                    PaymentProvider.MOCK,
                    BigDecimal.ZERO,
                    "ARS",
                    "FREE-TRIAL-" + userId,
                    start,
                    end,
                    null
            );
        }

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
                null
        );
    }

    @Override
    @Transactional
    public void cancelSubscription(String userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No se encontro suscripcion para cancelar."));

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(subscription.getProvider());
        strategy.cancelSubscription(subscription.getExternalSubscriptionId());
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));
        subscriptionRepository.save(subscription);
    }
}

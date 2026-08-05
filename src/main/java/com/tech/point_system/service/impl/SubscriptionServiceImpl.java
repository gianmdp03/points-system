package com.tech.point_system.service.impl;

import com.tech.point_system._enum.BillingPeriod;
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
import com.tech.point_system.service.SubscriptionService;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.payment.PaymentStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PaymentStrategyFactory paymentStrategyFactory;

    @Override
    @Transactional
    public SubscriptionResponseDTO subscribeCompanyAdmin(String userId, SubscriptionRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        subscriptionRepository.findByUserId(userId).ifPresent(existingSub -> {
            if (existingSub.getStatus() == SubscriptionStatus.ACTIVE) {
                throw new ConflictException("Ya tienes una suscripción activa.");
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
    public SubscriptionDetailDTO getMySubscription(String userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No posees una suscripción registrada."));

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

    @Override
    @Transactional
    public void cancelSubscription(String userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No se encontró suscripción para cancelar."));

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(subscription.getProvider());
        strategy.cancelSubscription(subscription.getExternalSubscriptionId());

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));
        subscriptionRepository.save(subscription);
    }
}
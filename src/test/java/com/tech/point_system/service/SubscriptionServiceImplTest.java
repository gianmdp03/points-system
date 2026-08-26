package com.tech.point_system.service;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.dto.subscription.SubscriptionUpgradeRequestDTO;
import com.tech.point_system.exception.BadRequestException;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.payment.PaymentStrategyFactory;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.repository.UserRepository;
import com.tech.point_system.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private PlanValidatorService planValidatorService;

    @Mock
    private PaymentStrategy paymentStrategy;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("usr-admin-1")
                .email("admin@test.com")
                .name("Admin User")
                .currentPlan(SubscriptionPlan.NONE)
                .build();
        user.setIsFreeTrialOver(false);
    }

    @Test
    void testSubscribeCompanyAdmin_ZeroTrust_SetsPendingStatus() {
        SubscriptionRequestDTO dto = new SubscriptionRequestDTO(
                SubscriptionPlan.PRO,
                PaymentProvider.MERCADO_PAGO,
                BillingPeriod.MONTHLY,
                null
        );

        when(userRepository.findById("usr-admin-1")).thenReturn(Optional.of(user));
        when(paymentStrategyFactory.getStrategy(PaymentProvider.MERCADO_PAGO)).thenReturn(paymentStrategy);

        SubscriptionResponseDTO gatewayResponse = new SubscriptionResponseDTO(
                null,
                SubscriptionPlan.PRO,
                SubscriptionStatus.PENDING,
                PaymentProvider.MERCADO_PAGO,
                new BigDecimal("19990.00"),
                "ARS",
                "https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=123",
                "SUB:usr-admin-1:PRO:MONTHLY:uuid-1"
        );
        when(paymentStrategy.createSubscription(user, dto)).thenReturn(gatewayResponse);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> {
            Subscription s = i.getArgument(0);
            s.setId(101L);
            return s;
        });

        SubscriptionResponseDTO result = subscriptionService.subscribeCompanyAdmin("usr-admin-1", dto);

        assertNotNull(result);
        assertEquals(SubscriptionStatus.PENDING, result.status());
        assertEquals("https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=123", result.checkoutUrl());

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();
        assertEquals(SubscriptionStatus.PENDING, saved.getStatus());
        assertEquals(SubscriptionPlan.PRO, saved.getPlan());
        assertFalse(user.getIsFreeTrialOver());
    }

    @Test
    void testSubscribeCompanyAdmin_ExtendSamePlan_AllowsPurchase() {
        user.setCurrentPlan(SubscriptionPlan.PRO);
        user.setPlanExpirationDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(15));

        SubscriptionRequestDTO dto = new SubscriptionRequestDTO(
                SubscriptionPlan.PRO,
                PaymentProvider.MERCADO_PAGO,
                BillingPeriod.MONTHLY,
                null
        );

        when(userRepository.findById("usr-admin-1")).thenReturn(Optional.of(user));
        when(paymentStrategyFactory.getStrategy(PaymentProvider.MERCADO_PAGO)).thenReturn(paymentStrategy);

        SubscriptionResponseDTO gatewayResponse = new SubscriptionResponseDTO(
                10L,
                SubscriptionPlan.PRO,
                SubscriptionStatus.PENDING,
                PaymentProvider.MERCADO_PAGO,
                new BigDecimal("19990.00"),
                "ARS",
                "https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=extend-123",
                "SUB:usr-admin-1:PRO:MONTHLY:uuid-extend"
        );
        when(paymentStrategy.createSubscription(user, dto)).thenReturn(gatewayResponse);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        SubscriptionResponseDTO result = subscriptionService.subscribeCompanyAdmin("usr-admin-1", dto);

        assertNotNull(result);
        assertEquals(SubscriptionStatus.PENDING, result.status());
        assertEquals("https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=extend-123", result.checkoutUrl());
    }

    @Test
    void testSubscribeCompanyAdmin_DifferentActivePlan_HigherTier_ThrowsBadRequest() {
        user.setCurrentPlan(SubscriptionPlan.BASIC);
        user.setPlanExpirationDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(15));

        SubscriptionRequestDTO dto = new SubscriptionRequestDTO(
                SubscriptionPlan.PRO,
                PaymentProvider.MERCADO_PAGO,
                BillingPeriod.MONTHLY,
                null
        );

        when(userRepository.findById("usr-admin-1")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () ->
                subscriptionService.subscribeCompanyAdmin("usr-admin-1", dto)
        );

        verify(paymentStrategyFactory, never()).getStrategy(any());
    }

    @Test
    void testUpgradeSubscription_ZeroTrust_DoesNotModifyPlanInDatabase() {
        user.setCurrentPlan(SubscriptionPlan.BASIC);
        user.setPlanExpirationDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(20));

        Subscription currentSub = Subscription.builder()
                .id(50L)
                .user(user)
                .plan(SubscriptionPlan.BASIC)
                .billingPeriod(BillingPeriod.MONTHLY)
                .price(new BigDecimal("9990.00"))
                .currency("ARS")
                .status(SubscriptionStatus.APPROVED)
                .provider(PaymentProvider.MERCADO_PAGO)
                .build();

        SubscriptionUpgradeRequestDTO dto = new SubscriptionUpgradeRequestDTO(
                SubscriptionPlan.ENTERPRISE
        );

        when(userRepository.findById("usr-admin-1")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findTopByUserIdOrderByIdDesc("usr-admin-1")).thenReturn(Optional.of(currentSub));
        doNothing().when(planValidatorService).validatePlanChangeEligibility("usr-admin-1", SubscriptionPlan.ENTERPRISE);
        when(paymentStrategyFactory.getStrategy(PaymentProvider.MERCADO_PAGO)).thenReturn(paymentStrategy);

        SubscriptionResponseDTO gatewayResponse = new SubscriptionResponseDTO(
                50L,
                SubscriptionPlan.ENTERPRISE,
                SubscriptionStatus.PENDING,
                PaymentProvider.MERCADO_PAGO,
                new BigDecimal("25000.00"),
                "ARS",
                "https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=upg-99",
                "UPG:usr-admin-1:ENTERPRISE:50:uuid-upg"
        );
        when(paymentStrategy.upgradeSubscription(currentSub, SubscriptionPlan.ENTERPRISE, dto)).thenReturn(gatewayResponse);

        SubscriptionResponseDTO result = subscriptionService.upgradeSubscription("usr-admin-1", dto);

        assertNotNull(result);
        assertEquals(SubscriptionPlan.ENTERPRISE, result.plan());
        assertEquals(SubscriptionStatus.PENDING, result.status());
        assertEquals("https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=upg-99", result.checkoutUrl());

        // Zero-Trust verification: user and subscription in memory/DB MUST remain BASIC
        assertEquals(SubscriptionPlan.BASIC, user.getCurrentPlan());
        assertEquals(SubscriptionPlan.BASIC, currentSub.getPlan());
        assertEquals(new BigDecimal("9990.00"), currentSub.getPrice());
        verify(subscriptionRepository, never()).save(any());
        assertFalse(user.getIsFreeTrialOver());
    }

    @Test
    void testUpgradeSubscription_SamePlan_ThrowsConflictException() {
        user.setCurrentPlan(SubscriptionPlan.PRO);

        when(userRepository.findById("usr-admin-1")).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () ->
                subscriptionService.upgradeSubscription("usr-admin-1", SubscriptionPlan.PRO)
        );
    }

    @Test
    void testCalculateDaysRemaining_ContinuousDurationImmuneToUtcMidnight() {
        // Suscripción comprada a las 20:00 (hora local Argentina, 23:00Z) por 120 días
        OffsetDateTime purchaseTime = OffsetDateTime.of(2026, 8, 25, 23, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime expirationDate = purchaseTime.plusDays(120);

        // 1 hora y 17 minutos después (00:17Z del día siguiente en UTC): DEBE seguir siendo 120 días
        OffsetDateTime twoHoursLater = purchaseTime.plusHours(1).plusMinutes(17);
        assertEquals(120L, SubscriptionServiceImpl.calculateDaysRemaining(twoHoursLater, expirationDate));

        // 23 horas y 59 minutos después: DEBE seguir siendo 120 días
        OffsetDateTime almostOneDayLater = purchaseTime.plusHours(23).plusMinutes(59);
        assertEquals(120L, SubscriptionServiceImpl.calculateDaysRemaining(almostOneDayLater, expirationDate));

        // 24 horas y 1 segundo después: Pasa a 119 días
        OffsetDateTime oneDayAndOneSecLater = purchaseTime.plusDays(1).plusSeconds(1);
        assertEquals(119L, SubscriptionServiceImpl.calculateDaysRemaining(oneDayAndOneSecLater, expirationDate));

        // A las últimas 2 horas de cobertura: DEBE indicar 1 día restante
        OffsetDateTime lastTwoHours = expirationDate.minusHours(2);
        assertEquals(1L, SubscriptionServiceImpl.calculateDaysRemaining(lastTwoHours, expirationDate));

        // En el momento exacto de vencimiento o posterior: 0 días
        assertEquals(0L, SubscriptionServiceImpl.calculateDaysRemaining(expirationDate, expirationDate));
        assertEquals(0L, SubscriptionServiceImpl.calculateDaysRemaining(expirationDate.plusMinutes(10), expirationDate));
    }
}




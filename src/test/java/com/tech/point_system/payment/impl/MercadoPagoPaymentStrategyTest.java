package com.tech.point_system.payment.impl;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;
import com.tech.point_system.config.MercadoPagoProperties;
import com.tech.point_system.dto.mercadopago.MpPreferenceModels.*;
import com.tech.point_system.dto.subscription.PlanConfigDTO;
import com.tech.point_system.dto.subscription.ProrationPreviewResponseDTO;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.payment.mercadopago.MercadoPagoPreferenceClient;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.repository.UserRepository;
import com.tech.point_system.service.ProrationCalculatorService;
import com.tech.point_system.service.SubscriptionPlanConfigService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MercadoPagoPaymentStrategyTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MercadoPagoPreferenceClient preferenceClient;

    @Mock
    private ProrationCalculatorService prorationCalculatorService;

    @Mock
    private SubscriptionPlanConfigService planConfigService;

    @Mock
    private MercadoPagoProperties properties;

    @InjectMocks
    private MercadoPagoPaymentStrategy strategy;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("usr-100")
                .email("store@mercadopago.com")
                .name("Test User")
                .build();
        user.setIsFreeTrialOver(false);
    }

    @Test
    void testGetProvider() {
        assertEquals(PaymentProvider.MERCADO_PAGO, strategy.getProvider());
    }

    @Test
    void testCreateSubscription_GeneratesPreference_ReturnsPendingWithCheckoutUrl() {
        SubscriptionRequestDTO dto = new SubscriptionRequestDTO(
                SubscriptionPlan.PRO,
                PaymentProvider.MERCADO_PAGO,
                BillingPeriod.MONTHLY,
                null
        );



        PlanConfigDTO planConfig = new PlanConfigDTO(
                SubscriptionPlan.PRO, "Plan Pro", "Desc",
                new BigDecimal("19990.00"), new BigDecimal("53990.00"), new BigDecimal("99990.00"), new BigDecimal("199990.00"),
                new BigDecimal("29.00"), new BigDecimal("79.00"), new BigDecimal("149.00"), new BigDecimal("290.00"),
                1000, -1, 3, true, true, List.of()
        );

        when(planConfigService.getPlanPrice(eq(SubscriptionPlan.PRO), eq(BillingPeriod.MONTHLY), eq("ARS")))
                .thenReturn(new BigDecimal("19990.00"));
        when(planConfigService.getPlanConfig(SubscriptionPlan.PRO)).thenReturn(planConfig);
        when(properties.getBackUrl()).thenReturn("http://localhost:4200/subscription/callback");
        when(properties.isSandbox()).thenReturn(true);


        MpPreferenceResponse mockPref = new MpPreferenceResponse(
                "PREF-12345",
                "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-12345",
                "https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-12345",
                "SUB:usr-100:PRO:MONTHLY:uuid-123"
        );

        when(preferenceClient.createPreference(any(MpPreferenceRequest.class))).thenReturn(mockPref);

        SubscriptionResponseDTO response = strategy.createSubscription(user, dto);

        assertNotNull(response);
        assertEquals(SubscriptionPlan.PRO, response.plan());
        assertEquals(SubscriptionStatus.PENDING, response.status());
        assertEquals("https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-12345", response.checkoutUrl());
        assertTrue(response.externalSubscriptionId().startsWith("SUB:usr-100:PRO:MONTHLY:"));
    }

    @Test
    void testUpgradeSubscription_PositiveProration_GeneratesPreference() {
        Subscription subscription = Subscription.builder()
                .id(10L)
                .user(user)
                .plan(SubscriptionPlan.BASIC)
                .billingPeriod(BillingPeriod.MONTHLY)
                .price(new BigDecimal("9990.00"))
                .currency("ARS")
                .externalSubscriptionId("PAY-100")
                .build();

        ProrationPreviewResponseDTO proration = new ProrationPreviewResponseDTO(
                SubscriptionPlan.BASIC,
                SubscriptionPlan.PRO,
                BillingPeriod.MONTHLY,
                30,
                15,
                new BigDecimal("9990.00"),
                new BigDecimal("19990.00"),
                new BigDecimal("333.00"),
                new BigDecimal("666.33"),
                new BigDecimal("5000.00"),
                "ARS"
        );

        PlanConfigDTO proConfig = new PlanConfigDTO(
                SubscriptionPlan.PRO, "Plan Pro", "Desc",
                new BigDecimal("19990.00"), new BigDecimal("53990.00"), new BigDecimal("99990.00"), new BigDecimal("199990.00"),
                new BigDecimal("29.00"), new BigDecimal("79.00"), new BigDecimal("149.00"), new BigDecimal("290.00"),
                1000, -1, 3, true, true, List.of()
        );

        when(prorationCalculatorService.calculateUpgradeProration(subscription, SubscriptionPlan.PRO)).thenReturn(proration);
        when(planConfigService.getPlanConfig(SubscriptionPlan.PRO)).thenReturn(proConfig);
        when(properties.getBackUrl()).thenReturn("http://localhost:4200/subscription/callback");
        when(properties.isSandbox()).thenReturn(false);


        MpPreferenceResponse mockPref = new MpPreferenceResponse(
                "PREF-UPG-999",
                "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-UPG-999",
                null,
                "UPG:usr-100:PRO:10:uuid-123"
        );

        when(preferenceClient.createPreference(any(MpPreferenceRequest.class))).thenReturn(mockPref);

        SubscriptionResponseDTO response = strategy.upgradeSubscription(subscription, SubscriptionPlan.PRO, null);

        assertNotNull(response);
        assertEquals(SubscriptionPlan.PRO, response.plan());
        assertEquals(SubscriptionStatus.PENDING, response.status());
        assertEquals("https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-UPG-999", response.checkoutUrl());
        assertTrue(response.externalSubscriptionId().startsWith("UPG:usr-100:PRO:10:"));
    }

    @Test
    void testProcessWebhook_ApprovedInitialSubscription_ActivatesSubscription() {
        MpPaymentResponse payment = new MpPaymentResponse(
                99887766L,
                "approved",
                "accredited",
                "SUB:usr-100:PRO:MONTHLY:1234-uuid",
                new BigDecimal("19990.00"),
                "2026-08-25T00:00:00Z",
                "visa",
                null
        );

        when(preferenceClient.getPayment("99887766")).thenReturn(payment);
        when(subscriptionRepository.findByExternalSubscriptionId("99887766")).thenReturn(Optional.empty());
        when(userRepository.findById("usr-100")).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        strategy.processWebhook(Map.of("data", Map.of("id", "99887766")));

        assertTrue(user.getIsFreeTrialOver());
        assertEquals(SubscriptionPlan.PRO, user.getCurrentPlan());
        assertNotNull(user.getPlanExpirationDate());

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();
        assertEquals(SubscriptionStatus.APPROVED, saved.getStatus());
        assertEquals(SubscriptionPlan.PRO, saved.getPlan());
        assertEquals(BillingPeriod.MONTHLY, saved.getBillingPeriod());
        assertEquals("99887766", saved.getExternalSubscriptionId());
    }

    @Test
    void testProcessWebhook_ApprovedExtension_AccumulatesDays() {
        OffsetDateTime futureBilling = OffsetDateTime.now(ZoneOffset.UTC).plusDays(15);
        user.setCurrentPlan(SubscriptionPlan.PRO);
        user.setPlanExpirationDate(futureBilling);

        Subscription existingActiveSub = Subscription.builder()
                .id(10L)
                .user(user)
                .plan(SubscriptionPlan.PRO)
                .billingPeriod(BillingPeriod.MONTHLY)
                .status(SubscriptionStatus.APPROVED)
                .externalSubscriptionId("OLD-PAY-1")
                .build();

        MpPaymentResponse payment = new MpPaymentResponse(
                11229988L,
                "approved",
                "accredited",
                "SUB:usr-100:PRO:MONTHLY:uuid-extend",
                new BigDecimal("19990.00"),
                "2026-08-25T00:00:00Z",
                "visa",
                null
        );

        when(preferenceClient.getPayment("11229988")).thenReturn(payment);
        when(subscriptionRepository.findByExternalSubscriptionId("11229988")).thenReturn(Optional.empty());
        when(userRepository.findById("usr-100")).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        strategy.processWebhook(Map.of("data", Map.of("id", "11229988")));

        // Verifica que la nueva fecha de vencimiento sumó 30 días a la fecha futura que ya tenía en el usuario (15 + 30 = 45 días)
        assertEquals(futureBilling.plusDays(30), user.getPlanExpirationDate());
        assertEquals(SubscriptionPlan.PRO, user.getCurrentPlan());
    }

    @Test
    void testProcessWebhook_ApprovedUpgrade_UpdatesPlanAndPreservesExpirationDate() {
        OffsetDateTime existingExpiration = OffsetDateTime.now(ZoneOffset.UTC).plusDays(15);
        user.setCurrentPlan(SubscriptionPlan.BASIC);
        user.setPlanExpirationDate(existingExpiration);

        Subscription existingSub = Subscription.builder()
                .id(15L)
                .user(user)
                .plan(SubscriptionPlan.BASIC)
                .billingPeriod(BillingPeriod.MONTHLY)
                .price(new BigDecimal("9990.00"))
                .currency("ARS")
                .status(SubscriptionStatus.APPROVED)
                .build();

        MpPaymentResponse payment = new MpPaymentResponse(
                55443322L,
                "approved",
                "accredited",
                "UPG:usr-100:ENTERPRISE:15:uuid-789",
                new BigDecimal("15000.00"),
                "2026-08-25T00:00:00Z",
                "master",
                null
        );

        when(preferenceClient.getPayment("55443322")).thenReturn(payment);
        when(subscriptionRepository.findByExternalSubscriptionId("55443322")).thenReturn(Optional.empty());
        when(userRepository.findById("usr-100")).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        strategy.processWebhook(Map.of("data", Map.of("id", "55443322")));

        assertEquals(SubscriptionPlan.ENTERPRISE, user.getCurrentPlan());
        assertEquals(existingExpiration, user.getPlanExpirationDate());
        assertTrue(user.getIsFreeTrialOver());
    }

    @Test
    void testProcessWebhook_RejectedSubscription_SetsPaymentFailed() {
        Subscription pendingSub = Subscription.builder()
                .id(20L)
                .user(user)
                .plan(SubscriptionPlan.PRO)
                .status(SubscriptionStatus.PENDING)
                .build();

        MpPaymentResponse payment = new MpPaymentResponse(
                11223344L,
                "rejected",
                "cc_rejected_insufficient_amount",
                "SUB:usr-100:PRO:MONTHLY:uuid-fail",
                new BigDecimal("19990.00"),
                null,
                "visa",
                null
        );

        when(preferenceClient.getPayment("11223344")).thenReturn(payment);
        when(subscriptionRepository.findByExternalSubscriptionId("11223344")).thenReturn(Optional.empty());
        when(subscriptionRepository.findByUserId("usr-100")).thenReturn(Optional.of(pendingSub));

        strategy.processWebhook(Map.of("id", "11223344"));

        assertEquals(SubscriptionStatus.PAYMENT_FAILED, pendingSub.getStatus());
        verify(subscriptionRepository).save(pendingSub);
    }

    @Test
    void testProcessWebhook_RefundedPayment_RevokesAccess() {
        Subscription activeSub = Subscription.builder()
                .id(30L)
                .user(user)
                .plan(SubscriptionPlan.PRO)
                .status(SubscriptionStatus.APPROVED)
                .externalSubscriptionId("778899")
                .build();

        MpPaymentResponse payment = new MpPaymentResponse(
                778899L,
                "refunded",
                "refunded",
                "SUB:usr-100:PRO:MONTHLY:uuid-refund",
                new BigDecimal("19990.00"),
                null,
                "visa",
                null
        );

        when(preferenceClient.getPayment("778899")).thenReturn(payment);
        when(subscriptionRepository.findByExternalSubscriptionId("778899")).thenReturn(Optional.of(activeSub));

        strategy.processWebhook(Map.of("data.id", "778899"));

        assertEquals(SubscriptionStatus.EXPIRED, activeSub.getStatus());
        assertEquals(SubscriptionPlan.NONE, user.getCurrentPlan());
        verify(subscriptionRepository).save(activeSub);
    }


    @Test
    void testProcessWebhook_Idempotency_AlreadyProcessedPaymentDoesNotDuplicate() {
        Subscription activeSub = Subscription.builder()
                .id(40L)
                .user(user)
                .plan(SubscriptionPlan.PRO)
                .status(SubscriptionStatus.APPROVED)
                .externalSubscriptionId("999111")
                .build();

        MpPaymentResponse payment = new MpPaymentResponse(
                999111L,
                "approved",
                "accredited",
                "SUB:usr-100:PRO:MONTHLY:uuid-already-done",
                new BigDecimal("19990.00"),
                null,
                "visa",
                null
        );

        when(preferenceClient.getPayment("999111")).thenReturn(payment);
        when(subscriptionRepository.findByExternalSubscriptionId("999111")).thenReturn(Optional.of(activeSub));

        strategy.processWebhook(Map.of("data", Map.of("id", "999111")));

        // Verify no updates or modifications were re-executed
        verify(userRepository, never()).findById(any());
        verify(subscriptionRepository, never()).save(any());
    }
}


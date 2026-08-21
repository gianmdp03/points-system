package com.tech.point_system.payment.impl;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;
import com.tech.point_system.config.MercadoPagoProperties;
import com.tech.point_system.dto.mercadopago.MercadoPagoPreapprovalRequest;
import com.tech.point_system.dto.mercadopago.MercadoPagoPreapprovalResponse;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.service.mercadopago.MercadoPagoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoPaymentStrategyTest {

    @Mock
    private MercadoPagoClient mercadoPagoClient;

    @Mock
    private MercadoPagoProperties properties;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private MercadoPagoPaymentStrategy strategy;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-uuid-123");
        testUser.setEmail("test@example.com");
    }

    @Test
    void shouldReturnMercadoPagoProvider() {
        assertEquals(PaymentProvider.MERCADO_PAGO, strategy.getProvider());
    }

    @Test
    void shouldCreateSubscriptionSuccessfully() {
        when(properties.getBackUrl()).thenReturn("http://localhost:3000/callback");

        MercadoPagoPreapprovalResponse mockResponse = new MercadoPagoPreapprovalResponse(
                "mp-sub-123",
                12345L,
                "test@example.com",
                "http://localhost:3000/callback",
                null,
                null,
                "pending",
                "Pointly - Plan BASIC",
                "user-uuid-123",
                null,
                null,
                "https://www.mercadopago.com.ar/checkout/mp-sub-123",
                null,
                null
        );

        when(mercadoPagoClient.createPreapproval(any(MercadoPagoPreapprovalRequest.class))).thenReturn(mockResponse);

        SubscriptionRequestDTO dto = new SubscriptionRequestDTO(
                SubscriptionPlan.BASIC,
                PaymentProvider.MERCADO_PAGO,
                BillingPeriod.MONTHLY,
                null,
                null
        );

        SubscriptionResponseDTO response = strategy.createSubscription(testUser, dto);

        assertNotNull(response);
        assertEquals(SubscriptionPlan.BASIC, response.plan());
        assertEquals(PaymentProvider.MERCADO_PAGO, response.provider());
        assertEquals("mp-sub-123", response.externalSubscriptionId());
        assertEquals("https://www.mercadopago.com.ar/checkout/mp-sub-123", response.checkoutUrl());
        assertEquals(new BigDecimal("9900.00"), response.price());
    }

    @Test
    void shouldCancelSubscription() {
        strategy.cancelSubscription("mp-sub-123");
        verify(mercadoPagoClient).cancelPreapproval("mp-sub-123");
    }

    @Test
    void shouldProcessWebhookAndReconcileSubscription() {
        Subscription subscription = Subscription.builder()
                .externalSubscriptionId("mp-sub-123")
                .status(SubscriptionStatus.PENDING)
                .build();

        when(subscriptionRepository.findByExternalSubscriptionId("mp-sub-123"))
                .thenReturn(Optional.of(subscription));

        MercadoPagoPreapprovalResponse mpResponse = new MercadoPagoPreapprovalResponse(
                "mp-sub-123",
                12345L,
                "test@example.com",
                null,
                null,
                null,
                "authorized",
                "Pointly - Plan BASIC",
                "user-uuid-123",
                null,
                null,
                null,
                null,
                null
        );

        when(mercadoPagoClient.getPreapproval("mp-sub-123")).thenReturn(mpResponse);

        Map<String, Object> payload = Map.of(
                "type", "subscription_preapproval",
                "data", Map.of("id", "mp-sub-123")
        );

        strategy.processWebhook(payload);

        verify(subscriptionRepository).save(subscription);
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    void shouldMapStatusesCorrectly() {
        assertEquals(SubscriptionStatus.ACTIVE, MercadoPagoPaymentStrategy.mapStatus("authorized"));
        assertEquals(SubscriptionStatus.ACTIVE, MercadoPagoPaymentStrategy.mapStatus("active"));
        assertEquals(SubscriptionStatus.PENDING, MercadoPagoPaymentStrategy.mapStatus("pending"));
        assertEquals(SubscriptionStatus.CANCELLED, MercadoPagoPaymentStrategy.mapStatus("cancelled"));
        assertEquals(SubscriptionStatus.CANCELLED, MercadoPagoPaymentStrategy.mapStatus("canceled"));
        assertEquals(SubscriptionStatus.PAYMENT_FAILED, MercadoPagoPaymentStrategy.mapStatus("paused"));
        assertEquals(SubscriptionStatus.PAYMENT_FAILED, MercadoPagoPaymentStrategy.mapStatus("rejected"));
        assertEquals(SubscriptionStatus.PENDING, MercadoPagoPaymentStrategy.mapStatus(null));
    }
}

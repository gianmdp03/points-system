package com.tech.point_system.service;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.dto.subscription.ProrationPreviewResponseDTO;
import com.tech.point_system.exception.BadRequestException;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProrationCalculatorServiceTest {

    @Mock
    private SubscriptionPlanConfigService planConfigService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProrationCalculatorService prorationCalculatorService;

    private Subscription basicSubscription;
    private User testUser;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        testUser = User.builder()
                .id("usr-123")
                .email("admin@store.com")
                .currentPlan(SubscriptionPlan.BASIC)
                .planExpirationDate(now.plusDays(15))
                .build();

        basicSubscription = Subscription.builder()
                .id(1L)
                .user(testUser)
                .plan(SubscriptionPlan.BASIC)
                .billingPeriod(BillingPeriod.MONTHLY)
                .price(new BigDecimal("10000.00"))
                .currency("ARS")
                .startDate(now.minusDays(15))
                .build();
    }

    @Test
    void testCalculateUpgradeProration_Success() {
        when(planConfigService.getPlanPrice(eq(SubscriptionPlan.BASIC), eq(BillingPeriod.MONTHLY), eq("ARS")))
                .thenReturn(new BigDecimal("10000.00"));
        when(planConfigService.getPlanPrice(eq(SubscriptionPlan.PRO), eq(BillingPeriod.MONTHLY), eq("ARS")))
                .thenReturn(new BigDecimal("20000.00"));

        ProrationPreviewResponseDTO result = prorationCalculatorService.calculateUpgradeProration(basicSubscription, SubscriptionPlan.PRO);

        assertNotNull(result);
        assertEquals(SubscriptionPlan.BASIC, result.currentPlan());
        assertEquals(SubscriptionPlan.PRO, result.newPlan());
        assertEquals(30, result.totalDaysInPeriod());
        assertEquals(15, result.remainingDays());
        assertEquals(new BigDecimal("10000.00"), result.currentPlanPrice());
        assertEquals(new BigDecimal("20000.00"), result.newPlanPrice());
        assertEquals(new BigDecimal("5000.00"), result.proratedUpgradeAmount());
    }

    @Test
    void testCalculateUpgradeProration_SamePlan_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                prorationCalculatorService.calculateUpgradeProration(basicSubscription, SubscriptionPlan.BASIC));
    }

    @Test
    void testCalculateUpgradeProration_DowngradeWithRemainingDays_ThrowsConflictException() {
        assertThrows(ConflictException.class, () ->
                prorationCalculatorService.calculateUpgradeProration(basicSubscription, SubscriptionPlan.FREE_TRIAL));
    }
}

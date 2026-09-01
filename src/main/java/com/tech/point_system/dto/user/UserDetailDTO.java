package com.tech.point_system.dto.user;

import com.tech.point_system._enum.Role;
import com.tech.point_system._enum.SubscriptionPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record UserDetailDTO(
        String id,
        String email,
        String name,
        String dni,
        Role role,
        Boolean isFreeTrialOver,
        LocalDate freeTrialStartTime,
        LocalDate freeTrialEndTime,
        SubscriptionPlan currentPlan,
        OffsetDateTime planExpirationDate,
        Boolean isSuspendedForChargeback,
        BigDecimal pendingDebtArs
) {}


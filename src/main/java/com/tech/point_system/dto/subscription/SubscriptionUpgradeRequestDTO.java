package com.tech.point_system.dto.subscription;

import com.tech.point_system._enum.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record SubscriptionUpgradeRequestDTO(
        @NotNull(message = "El nuevo plan es obligatorio")
        SubscriptionPlan newPlan
) {}


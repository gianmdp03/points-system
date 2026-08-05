package com.tech.point_system.dto.subscription;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubscriptionRequestDTO(
        @NotNull(message = "El plan es obligatorio")
        SubscriptionPlan plan,

        @NotNull(message = "El proveedor de pago es obligatorio")
        PaymentProvider provider,

        @NotNull(message = "El periodo de facturación es obligatorio")
        BillingPeriod billingPeriod,

        @Size(max = 500)
        String returnUrl,

        Long companyId
) {}
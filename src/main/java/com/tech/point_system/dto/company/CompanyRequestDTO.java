package com.tech.point_system.dto.company;

import com.tech.point_system.extra.CompanyDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompanyRequestDTO(
        @NotBlank @Size(max = 300) String name,
        @Valid CompanyDetails companyDetails,
        @Positive BigDecimal amountStep,
        @Positive Integer pointsPerStep,
        Boolean isPointsExpirationEnabled,
        @Positive Integer pointsExpirationDays,
        Boolean isInactiveClientPurgeEnabled,
        @Positive Integer inactiveClientPurgeDays,
        Boolean isClientRetentionEnabled,
        @Positive Integer clientRetentionDays
) {
    @AssertTrue(message = "Si el vencimiento de puntos está habilitado, los días de vencimiento deben ser mayores a 0.")
    public boolean isExpirationDaysValid() {
        if (Boolean.TRUE.equals(isPointsExpirationEnabled)) {
            return pointsExpirationDays != null && pointsExpirationDays > 0;
        }
        return true;
    }

    @AssertTrue(message = "Si la limpieza de clientes inactivos está habilitada, los días de inactividad deben ser mayores a 0.")
    public boolean isInactiveClientPurgeDaysValid() {
        if (Boolean.TRUE.equals(isInactiveClientPurgeEnabled)) {
            return inactiveClientPurgeDays != null && inactiveClientPurgeDays > 0;
        }
        return true;
    }

    @AssertTrue(message = "Si la retención de clientes está habilitada, los días de intervalo deben ser mayores a 0.")
    public boolean isClientRetentionDaysValid() {
        if (Boolean.TRUE.equals(isClientRetentionEnabled)) {
            return clientRetentionDays != null && clientRetentionDays > 0;
        }
        return true;
    }
}

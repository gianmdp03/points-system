package com.tech.point_system.dto.company;

import com.tech.point_system._enum.AppAdminOwner;
import com.tech.point_system.extra.CompanyDetails;

import java.math.BigDecimal;

public record CompanyListDTO(
        Long id,
        String name,
        CompanyDetails companyDetails,
        BigDecimal amountStep,
        Integer pointsPerStep,
        Boolean isEnabled,
        AppAdminOwner appAdminOwner,
        Boolean isPointsExpirationEnabled,
        Integer pointsExpirationDays,
        Boolean isInactiveClientPurgeEnabled,
        Integer inactiveClientPurgeDays,
        Boolean isClientRetentionEnabled,
        Integer clientRetentionDays
) {
}

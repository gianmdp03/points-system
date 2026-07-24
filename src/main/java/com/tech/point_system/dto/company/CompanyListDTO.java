package com.tech.point_system.dto.company;

import com.tech.point_system.extra.CompanyDetails;

import java.math.BigDecimal;

public record CompanyListDTO(Long id, String name, CompanyDetails companyDetails, BigDecimal amountStep, Integer pointsPerStep, Boolean isEnabled) {
}
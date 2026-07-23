package com.tech.point_system.dto.company;

import com.tech.point_system.extra.CompanyDetails;
import jakarta.persistence.Column;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompanyRequestDTO(@NotBlank @Size(max = 300) String name, @Valid CompanyDetails companyDetails, @Positive BigDecimal amountStep, @Positive Integer pointsPerStep) {
}

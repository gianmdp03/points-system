package com.tech.point_system.dto.company;

import com.tech.point_system.extra.CompanyDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompanyUpdateDTO(@Size(max = 300) String name, @Valid CompanyDetails companyDetails, @Positive BigDecimal amountStep, @Positive Integer pointsPerStep) {
}

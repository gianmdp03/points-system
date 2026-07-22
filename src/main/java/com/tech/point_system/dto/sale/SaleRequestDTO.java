package com.tech.point_system.dto.sale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
public record SaleRequestDTO(@NotNull BigDecimal amount, @NotNull Long companyId, @NotBlank String userDni) {}

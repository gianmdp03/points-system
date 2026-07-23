package com.tech.point_system.dto.promotion;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PromotionRequestDTO(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @NotNull @FutureOrPresent OffsetDateTime startDate,
    @NotNull @FutureOrPresent OffsetDateTime endDate,
    @NotNull @Min(1) BigDecimal multiplier,
    @NotNull Long companyId) {}

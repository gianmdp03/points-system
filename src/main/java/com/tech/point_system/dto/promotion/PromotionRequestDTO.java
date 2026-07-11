package com.tech.point_system.dto.promotion;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record PromotionRequestDTO(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @NotNull @FutureOrPresent OffsetDateTime startDate,
    @NotNull @FutureOrPresent OffsetDateTime endDate,
    @NotNull Long companyId) {}

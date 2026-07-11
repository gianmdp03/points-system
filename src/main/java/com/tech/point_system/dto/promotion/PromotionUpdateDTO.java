package com.tech.point_system.dto.promotion;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record PromotionUpdateDTO(
    @Size(max = 100) String name,
    @Size(max = 500) String description,
    @FutureOrPresent OffsetDateTime startDate,
    @FutureOrPresent OffsetDateTime endDate) {}

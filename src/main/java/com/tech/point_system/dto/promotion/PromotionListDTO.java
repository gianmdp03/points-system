package com.tech.point_system.dto.promotion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PromotionListDTO(
    Long id,
    String name,
    String description,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    boolean isEnabled,
    BigDecimal multiplier) {}

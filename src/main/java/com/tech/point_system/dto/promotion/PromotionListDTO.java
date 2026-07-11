package com.tech.point_system.dto.promotion;

import java.time.OffsetDateTime;

public record PromotionListDTO(
    Long id,
    String name,
    String description,
    boolean isEnabled,
    OffsetDateTime startDate,
    OffsetDateTime endDate) {}

package com.tech.point_system.dto.promotion;

import com.tech.point_system.dto.company.CompanyListDTO;

import java.time.OffsetDateTime;

public record PromotionDetailDTO(
    Long id,
    String name,
    String description,
    boolean isEnabled,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    CompanyListDTO company) {}

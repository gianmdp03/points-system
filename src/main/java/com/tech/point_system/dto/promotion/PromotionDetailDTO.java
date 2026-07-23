package com.tech.point_system.dto.promotion;

import com.tech.point_system.dto.company.CompanyListDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PromotionDetailDTO(
    Long id,
    String name,
    String description,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    BigDecimal multiplier,
    CompanyListDTO company) {}

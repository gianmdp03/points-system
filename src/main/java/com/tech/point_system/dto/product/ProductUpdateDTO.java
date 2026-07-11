package com.tech.point_system.dto.product;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductUpdateDTO(
    @Size(max = 100) String name,
    @Size(max = 500) String description,
    @Positive BigDecimal price,
    @Size(max = 1000) String image) {}

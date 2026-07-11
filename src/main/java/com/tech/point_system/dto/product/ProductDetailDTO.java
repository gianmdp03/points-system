package com.tech.point_system.dto.product;

import com.tech.point_system.dto.company.CompanyListDTO;

import java.math.BigDecimal;

public record ProductDetailDTO(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String image,
    CompanyListDTO company) {}

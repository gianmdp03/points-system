package com.tech.point_system.dto.product;

import java.math.BigDecimal;

public record ProductListDTO(Long id, String name, String description, BigDecimal price, String image) {
}

package com.tech.point_system.dto.sale;

import com.tech.point_system.dto.client.ClientDetailDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SaleListDTO(
        Long id,
        BigDecimal amount,
        ClientDetailDTO client,
        OffsetDateTime createdAt
) {}

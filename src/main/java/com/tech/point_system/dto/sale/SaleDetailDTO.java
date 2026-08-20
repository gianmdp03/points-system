package com.tech.point_system.dto.sale;

import com.tech.point_system.dto.client.ClientDetailDTO;
import com.tech.point_system.dto.company.CompanyListDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SaleDetailDTO(
        Long id,
        BigDecimal amount,
        CompanyListDTO company,
        ClientDetailDTO client,
        OffsetDateTime createdAt
) {}

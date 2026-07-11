package com.tech.point_system.dto.sale;

import com.tech.point_system.dto.company.CompanyListDTO;

import java.math.BigDecimal;

public record SaleDetailDTO(Long id, BigDecimal amount, CompanyListDTO company) {}
//falta user
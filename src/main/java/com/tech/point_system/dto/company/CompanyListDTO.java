package com.tech.point_system.dto.company;

import com.tech.point_system.extra.CompanyDetails;

public record CompanyListDTO(Long id, String name, CompanyDetails companyDetails) {
}
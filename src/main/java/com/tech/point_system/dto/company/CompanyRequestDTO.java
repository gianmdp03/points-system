package com.tech.point_system.dto.company;

import com.tech.point_system.extra.CompanyDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

public record CompanyRequestDTO(@NotBlank @Size(max = 300) String name, @Valid CompanyDetails companyDetails) {
}

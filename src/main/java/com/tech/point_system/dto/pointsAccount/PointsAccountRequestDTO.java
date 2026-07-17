package com.tech.point_system.dto.pointsAccount;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PointsAccountRequestDTO(
    @NotNull Long companyId,
    @NotBlank @Email @Size(max = 200) String email,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 20) String dni) {}

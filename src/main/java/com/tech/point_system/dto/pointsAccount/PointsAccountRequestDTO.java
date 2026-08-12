package com.tech.point_system.dto.pointsAccount;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PointsAccountRequestDTO(
        @NotNull Long companyId,
        @NotBlank @Size(max = 20) String dni,
        @NotBlank @Size(max = 50) String country,
        @NotBlank String name,
        @Email String email,
        String phone
) {}

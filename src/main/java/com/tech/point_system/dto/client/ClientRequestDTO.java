package com.tech.point_system.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequestDTO(
        @NotBlank @Size(max = 20) String dni,
        @NotBlank @Size(max = 50) String country,
        @NotBlank String name,
        @Email String email,
        String phone
) {}
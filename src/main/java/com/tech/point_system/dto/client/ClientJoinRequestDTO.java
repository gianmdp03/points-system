package com.tech.point_system.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClientJoinRequestDTO(
        @NotNull(message = "El ID de la empresa es obligatorio")
        Long companyId,

        @NotBlank(message = "El DNI o documento es obligatorio")
        @Size(max = 20, message = "El DNI no puede superar los 20 caracteres")
        String dni,

        @NotBlank(message = "El país es obligatorio")
        @Size(max = 50, message = "El país no puede superar los 50 caracteres")
        String country,

        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @Email(message = "El formato de correo no es válido")
        String email,

        String phone
) {}

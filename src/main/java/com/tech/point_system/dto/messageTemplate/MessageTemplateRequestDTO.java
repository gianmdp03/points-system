package com.tech.point_system.dto.messageTemplate;

import com.tech.point_system._enum.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageTemplateRequestDTO(
        @NotBlank(message = "El nombre de la plantilla es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String name,

        @NotNull(message = "El tipo de notificación es obligatorio")
        NotificationType type,

        @Size(max = 200, message = "El asunto no puede superar los 200 caracteres")
        String subject,

        @NotBlank(message = "El contenido del mensaje es obligatorio")
        @Size(max = 4000, message = "El contenido no puede superar los 4000 caracteres")
        String content,

        @NotNull(message = "El ID de la empresa es obligatorio")
        Long companyId
) {}
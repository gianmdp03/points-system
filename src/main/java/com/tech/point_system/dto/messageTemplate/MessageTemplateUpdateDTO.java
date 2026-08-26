package com.tech.point_system.dto.messageTemplate;

import com.tech.point_system._enum.NotificationType;
import jakarta.validation.constraints.Size;

public record MessageTemplateUpdateDTO(
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String name,

        NotificationType type,

        @Size(max = 200, message = "El asunto no puede superar los 200 caracteres")
        String subject,

        @Size(max = 4000, message = "El contenido no puede superar los 4000 caracteres")
        String content
) {}
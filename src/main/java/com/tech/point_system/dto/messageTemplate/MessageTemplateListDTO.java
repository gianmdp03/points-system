package com.tech.point_system.dto.messageTemplate;

import com.tech.point_system._enum.NotificationType;

public record MessageTemplateListDTO(
        Long id,
        String name,
        NotificationType type,
        String subject,
        String content,
        Boolean isEnabled
) {}
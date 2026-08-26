package com.tech.point_system.dto.messageTemplate;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.dto.company.CompanyListDTO;

public record MessageTemplateDetailDTO(
        Long id,
        String name,
        NotificationType type,
        String subject,
        String content,
        Boolean isEnabled,
        CompanyListDTO company
) {}
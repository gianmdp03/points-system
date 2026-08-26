package com.tech.point_system.dto.client;

import jakarta.validation.constraints.NotNull;

public record ClientNotificationToggleDTO(
        @NotNull(message = "El estado de notificaciones no puede ser nulo")
        Boolean isNotificationEnabled
) {}

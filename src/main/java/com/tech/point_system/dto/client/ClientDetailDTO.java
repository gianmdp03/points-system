package com.tech.point_system.dto.client;

public record ClientDetailDTO(
        Long id,
        String dni,
        String country,
        String name,
        String email,
        String phone,
        Boolean isNotificationEnabled
) {}
package com.tech.point_system.dto.user;

import com.tech.point_system._enum.Role;

public record UserDetailDTO(String id, String email, String name, String dni, Role role) {}

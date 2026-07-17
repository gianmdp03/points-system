package com.tech.point_system.security.user.dto.user;

import com.tech.point_system.security.user._enum.Role;

public record UserDetailDTO(String id, String email, String name, String dni, Role role) {}

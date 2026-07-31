package com.tech.point_system.security.user.service;

import com.tech.point_system.security.user.dto.user.UserDetailDTO;

public interface UserService {
    UserDetailDTO getUserById(String supabaseUserId);
}

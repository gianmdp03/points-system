package com.tech.point_system.service;

import com.tech.point_system.dto.user.UserDetailDTO;

public interface UserService {
    UserDetailDTO getUserById(String supabaseUserId);
    UserDetailDTO enableFreeTrial(String userId);
}

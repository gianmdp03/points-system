package com.tech.point_system.service.impl;

import com.tech.point_system.dto.user.UserDetailDTO;
import com.tech.point_system.mapper.UserMapper;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.UserRepository;
import com.tech.point_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDetailDTO getUserById(String supabaseUserId) {
        User user = userRepository.findById(supabaseUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + supabaseUserId));
        return userMapper.toDetailDTO(user);
    }
}

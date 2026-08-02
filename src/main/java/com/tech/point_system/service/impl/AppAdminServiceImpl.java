package com.tech.point_system.service.impl;

import com.tech.point_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppAdminServiceImpl {
    private final UserRepository userRepository;
}

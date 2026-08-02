package com.tech.point_system.controller;

import com.tech.point_system.dto.user.UserDetailDTO;
import com.tech.point_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDetailDTO> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getUserById(jwt.getSubject()));
    }


}

package com.tech.point_system.controller;

import com.tech.point_system.service.AppAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app-admin")
@PreAuthorize("hasRole('APP_ADMIN')")
@RequiredArgsConstructor
public class AppAdminController {
    private final AppAdminService service;


}

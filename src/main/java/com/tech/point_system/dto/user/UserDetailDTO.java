package com.tech.point_system.dto.user;

import com.tech.point_system._enum.Role;

import java.time.LocalDate;

public record UserDetailDTO(String id, String email, String name, String dni, Role role, Boolean isFreeTrialOver, LocalDate freeTrialStartTime, LocalDate freeTrialEndTime) {}

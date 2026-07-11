package com.tech.point_system.dto.reward;

import com.tech.point_system.dto.company.CompanyListDTO;

public record RewardDetailDTO(Long id, String name, String description, Integer pointsToEarn, CompanyListDTO company) {
}

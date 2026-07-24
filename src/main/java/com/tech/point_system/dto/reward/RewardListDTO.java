package com.tech.point_system.dto.reward;

public record RewardListDTO(Long id, String name, String description, Integer costInPoints, Boolean isEnabled) {
}

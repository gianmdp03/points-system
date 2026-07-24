package com.tech.point_system.dto.reward;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RewardRequestDTO(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @NotNull @Positive Integer costInPoints,
    @NotNull Long companyId) {}

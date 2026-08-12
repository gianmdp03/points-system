package com.tech.point_system.dto.reward;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RewardRedeemDTO(
        @NotNull Long companyId,
        @NotNull Long rewardId,
        @NotBlank String dni,
        @NotBlank String country
) {}
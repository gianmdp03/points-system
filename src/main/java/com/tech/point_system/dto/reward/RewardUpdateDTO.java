package com.tech.point_system.dto.reward;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RewardUpdateDTO(@Size(max = 100) String name,
                              @Size(max = 500) String description,
                              @Positive Integer pointsToEarn) {
}

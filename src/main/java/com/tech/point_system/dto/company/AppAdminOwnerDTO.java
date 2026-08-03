package com.tech.point_system.dto.company;

import com.tech.point_system._enum.AppAdminOwner;
import jakarta.validation.constraints.NotNull;

public record AppAdminOwnerDTO(@NotNull Long companyId, @NotNull AppAdminOwner appAdminOwner) {}

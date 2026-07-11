package com.tech.point_system.extra;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyDetails(@NotBlank @Size(max = 300) String country, @NotBlank @Size(max = 300) String province, @NotBlank @Size(max = 300) String city, @NotBlank @Size(max = 200) String address, @NotBlank @Size(max = 50) String zipCode) {
}

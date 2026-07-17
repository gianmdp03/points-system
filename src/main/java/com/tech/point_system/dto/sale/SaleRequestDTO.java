package com.tech.point_system.dto.sale;

import java.math.BigDecimal;

public record SaleRequestDTO(BigDecimal amount, Long companyId, String userDni) {}

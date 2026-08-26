package com.tech.point_system.dto.pointsAccount;

import com.tech.point_system.dto.client.ClientDetailDTO;
import com.tech.point_system.dto.company.CompanyListDTO;
import java.time.OffsetDateTime;

public record PointsAccountDetailDTO(
        Long id,
        Integer balance,
        OffsetDateTime lastActivityDate,
        CompanyListDTO company,
        ClientDetailDTO client
) {}
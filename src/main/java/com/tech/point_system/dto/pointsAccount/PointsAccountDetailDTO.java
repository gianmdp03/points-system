package com.tech.point_system.dto.pointsAccount;

import com.tech.point_system.dto.client.ClientDetailDTO;
import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.user.UserDetailDTO;

public record PointsAccountDetailDTO(
        Long id,
        Integer balance,
        CompanyListDTO company,
        ClientDetailDTO client
) {}
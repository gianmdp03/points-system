package com.tech.point_system.service;

import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;

public interface PointsAccountService {
    PointsAccountDetailDTO registerClientAndCreateAccount(PointsAccountRequestDTO dto);
}

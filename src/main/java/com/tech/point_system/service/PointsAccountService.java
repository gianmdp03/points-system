package com.tech.point_system.service;

import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointsAccountService {
    PointsAccountDetailDTO registerClientAndCreateAccount(String companyAdminId, PointsAccountRequestDTO dto);
    Page<PointsAccountDetailDTO> listPointsAccounts(String companyAdminId, Long companyId, Pageable pageable);
}

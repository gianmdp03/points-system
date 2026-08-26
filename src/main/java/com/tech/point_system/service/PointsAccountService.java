package com.tech.point_system.service;

import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import com.tech.point_system.dto.pointsTransaction.PointsTransactionDetailDTO;
import com.tech.point_system.event.RewardRedeemEvent;
import com.tech.point_system.event.SaleCreatedEvent;
import com.tech.point_system.model.PointsTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointsAccountService {
    PointsAccountDetailDTO registerClientAndCreateAccount(String companyAdminId, PointsAccountRequestDTO dto);
    Page<PointsAccountDetailDTO> listPointsAccounts(String companyAdminId, Long companyId, Pageable pageable);
    Page<PointsAccountDetailDTO> listInactiveClients(String companyAdminId, Long companyId, int days, Pageable pageable);
    Page<PointsTransactionDetailDTO> getTransactionHistory(Long clientId, Long companyId, Pageable pageable);
}

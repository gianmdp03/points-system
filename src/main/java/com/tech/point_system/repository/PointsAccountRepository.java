package com.tech.point_system.repository;

import com.tech.point_system.model.PointsAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
    Optional<PointsAccount> findByClientIdAndCompanyId(Long clientId, Long companyId);
    Page<PointsAccount> findByCompanyId(Long companyId, Pageable pageable);
    long countByCompanyId(Long companyId);
}

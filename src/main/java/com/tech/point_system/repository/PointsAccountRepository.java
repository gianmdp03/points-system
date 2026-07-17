package com.tech.point_system.repository;

import com.tech.point_system.model.PointsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
    Optional<PointsAccount> findByUserIdAndCompanyId(String userId, Long companyId);
}

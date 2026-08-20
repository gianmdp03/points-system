package com.tech.point_system.repository;

import com.tech.point_system.model.PointsAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
    @EntityGraph(attributePaths = {"client", "company"})
    Optional<PointsAccount> findByClientIdAndCompanyId(Long clientId, Long companyId);

    @EntityGraph(attributePaths = {"client", "company"})
    Page<PointsAccount> findByCompanyId(Long companyId, Pageable pageable);

    long countByCompanyId(Long companyId);

    List<PointsAccount> findByCompanyIdAndLastActivityDateBefore(Long companyId, OffsetDateTime thresholdDate);
}

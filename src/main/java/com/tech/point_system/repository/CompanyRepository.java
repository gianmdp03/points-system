package com.tech.point_system.repository;

import com.tech.point_system.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByIsEnabledFalseAndDisabledDateBefore(OffsetDateTime thresholdDate);
    List<Company> findAllByAdminId(String adminId);
    Page<Company> findByAdminId(String adminId, Pageable pageable);
    Page<Company> findByPointsAccountsClientId(Long clientId, Pageable pageable);
    List<Company> findByIsInactiveClientPurgeEnabledTrueAndInactiveClientPurgeDaysIsNotNull();
    List<Company> findByIsClientRetentionEnabledTrueAndClientRetentionDaysIsNotNull();
    long countByAdminId(String adminId);
    Optional<Company> findByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = {"admin"})
    Optional<Company> findById(Long id);
}

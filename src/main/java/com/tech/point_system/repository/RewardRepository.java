package com.tech.point_system.repository;

import com.tech.point_system.model.Reward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardRepository extends JpaRepository<Reward, Long> {
    Page<Reward> findByCompanyId(Long companyId, Pageable pageable);
    List<Reward> findByCompanyId(Long companyId);
    List<Reward> findByCompanyIdAndIsEnabledTrue(Long companyId);
    Optional<Reward> findByIdAndCompanyId(Long id, Long companyId);
    long countByCompanyId(Long companyId);
    long countByCompanyIdAndIsEnabledTrue(Long companyId);
}

package com.tech.point_system.repository;

import com.tech.point_system.model.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Page<Promotion> findByCompanyId(Long companyId, Pageable pageable);
}

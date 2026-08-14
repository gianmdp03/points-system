package com.tech.point_system.repository;

import com.tech.point_system.model.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Page<Promotion> findByCompanyId(Long companyId, Pageable pageable);

    List<Promotion> findByIsEnabledTrueAndEndDateBefore(OffsetDateTime now);

    @Query("SELECT p FROM Promotion p WHERE p.company.id = :companyId " +
            "AND p.isEnabled = true " +
            "AND p.startDate <= :now AND p.endDate >= :now")
    Optional<Promotion> findActivePromotion(
            @Param("companyId") Long companyId,
            @Param("now") OffsetDateTime now
    );

    @Query("SELECT p FROM Promotion p WHERE p.company.id = :companyId " +
            "AND p.isEnabled = true " +
            "AND p.startDate <= :now AND p.endDate >= :now")
    List<Promotion> findActivePromotions(
            @Param("companyId") Long companyId,
            @Param("now") OffsetDateTime now
    );

    Optional<Promotion> findByIdAndCompanyId(Long id, Long companyId);
}

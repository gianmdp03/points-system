package com.tech.point_system.repository;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.model.MessageTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {
    @EntityGraph(attributePaths = {"company"})
    Page<MessageTemplate> findByCompanyId(Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    Optional<MessageTemplate> findByIdAndCompanyId(Long id, Long companyId);

    Optional<MessageTemplate> findByCompanyIdAndTypeAndIsEnabledTrue(Long companyId, NotificationType type);

    long countByCompanyId(Long companyId);
}
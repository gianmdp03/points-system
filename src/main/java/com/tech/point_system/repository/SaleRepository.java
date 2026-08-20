package com.tech.point_system.repository;

import com.tech.point_system.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    @EntityGraph(attributePaths = {"client", "company"})
    Page<Sale> findByCompanyId(Long companyId, Pageable pageable);

    long countByCompanyId(Long companyId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE sales s SET client_id = c.id FROM clients c, users u WHERE s.client_id IS NULL AND s.user_id = u.id AND (c.dni = u.dni OR c.email = u.email)", nativeQuery = true)
    int linkLegacySalesByUserId();

    @Modifying
    @Transactional
    @Query(value = "UPDATE sales s SET client_id = (SELECT pa.client_id FROM points_accounts pa WHERE pa.company_id = s.company_id ORDER BY pa.id ASC LIMIT 1) WHERE s.client_id IS NULL AND EXISTS (SELECT 1 FROM points_accounts pa WHERE pa.company_id = s.company_id)", nativeQuery = true)
    int linkOrphanSalesToCompanyClient();

    @Modifying
    @Transactional
    @Query(value = "UPDATE sales SET created_at = NOW() WHERE created_at IS NULL", nativeQuery = true)
    int fixNullCreatedAt();
}

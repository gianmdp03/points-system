package com.tech.point_system.repository;

import com.tech.point_system.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCompanyId(Long companyId, Pageable pageable);
}

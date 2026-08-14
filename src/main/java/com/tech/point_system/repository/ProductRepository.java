package com.tech.point_system.repository;

import com.tech.point_system.model.Company;
import com.tech.point_system.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCompany(Company company, Pageable pageable);
    List<Product> findByCompany(Company company);
    List<Product> findByCompanyId(Long companyId);
    Optional<Product> findByIdAndCompanyId(Long id, Long companyId);
}

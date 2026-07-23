package com.tech.point_system.repository;

import com.tech.point_system.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByIsEnabledFalseAndDisabledDateBefore();
}

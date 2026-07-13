package com.tech.point_system.service;

import com.tech.point_system.dto.company.CompanyDetailDTO;
import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.company.CompanyRequestDTO;
import com.tech.point_system.dto.company.CompanyUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyService {
    CompanyDetailDTO addCompany(CompanyRequestDTO dto);
    CompanyDetailDTO updateCompany(Long id, CompanyUpdateDTO dto);
    Page<CompanyListDTO> listCompanies(Pageable pageable);
    CompanyDetailDTO getCompany(Long id);
    void deleteCompany(Long id);
}

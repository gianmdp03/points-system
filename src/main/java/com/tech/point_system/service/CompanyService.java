package com.tech.point_system.service;

import com.tech.point_system.dto.company.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyService {
    CompanyDetailDTO addCompany(String companyAdminId, CompanyRequestDTO dto);
    CompanyDetailDTO updateCompany(String companyAdminId, Long id, CompanyUpdateDTO dto);
    Page<CompanyListDTO> listCompanies(Pageable pageable);
    CompanyDetailDTO getCompanyById(Long id);
    void disableCompany(String companyAdminId, Long companyId);
    void enableCompany(String companyAdminId, Long companyId);

    Page<CompanyListDTO> listAdminCompanies(String adminId, Pageable pageable);
    Page<CompanyListDTO> listUserSubscribedCompanies(String userId, Pageable pageable);
    CompanyListDTO setAppAdminOwner(AppAdminOwnerDTO dto);
}

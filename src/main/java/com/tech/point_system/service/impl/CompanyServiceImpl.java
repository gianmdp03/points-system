package com.tech.point_system.service.impl;

import com.tech.point_system._enum.AppAdminOwner;
import com.tech.point_system.dto.company.*;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.CompanyMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.UserRepository;
import com.tech.point_system.service.CompanyAccessValidator;
import com.tech.point_system.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository repository;
    private final CompanyMapper mapper;
    private final UserRepository userRepository;
    private final CompanyAccessValidator accessValidator;

    @Override
    @Transactional
    public CompanyDetailDTO addCompany(String userDni, CompanyRequestDTO dto) {
        Company company = mapper.toEntity(dto);
        User user = userRepository.findByDni(userDni).orElseThrow(()-> new NotFoundException("Company Admin Not Found"));
        company.setAdmin(user);
        company = repository.save(company);
        return mapper.toDetailDTO(company);
    }

    @Override
    @Transactional
    public CompanyDetailDTO updateCompany(String companyAdminId, Long companyId, CompanyUpdateDTO dto) {
        Company company = accessValidator.validateAccess(companyId, companyAdminId);
        mapper.updateEntityFromDTO(dto, company);
        company = repository.save(company);
        return mapper.toDetailDTO(company);
    }

    @Override
    public Page<CompanyListDTO> listCompanies(Pageable pageable) {
        Page<Company> companies = repository.findAll(pageable);
        if(companies.isEmpty()) {
            return Page.empty();
        }
        return companies.map(mapper::toListDTO);
    }

    @Override
    public CompanyDetailDTO getCompanyById(Long id) {
        Company company = repository.findById(id).orElseThrow(() -> new NotFoundException("Company Not Found"));
        return mapper.toDetailDTO(company);
    }

    @Override
    @Transactional
    public void disableCompany(String companyAdminId, Long companyId) {
        Company company = accessValidator.validateAccess(companyId, companyAdminId);
        company.setIsEnabled(false);
        company.setDisabledDate(OffsetDateTime.now(ZoneOffset.UTC));
        repository.save(company);
    }

    @Override
    @Transactional
    public void enableCompany(String companyAdminId, Long companyId){
        Company company = accessValidator.validateAccess(companyId, companyAdminId);
        company.setIsEnabled(true);
        company.setDisabledDate(null);
    }

    @Override
    public Page<CompanyListDTO> listAdminCompanies(String adminId, Pageable pageable) {
        Page<Company> companies = repository.findByAdminId(adminId, pageable);
        if(companies.isEmpty()) {
            return Page.empty();
        }
        return companies.map(mapper::toListDTO);
    }

    @Override
    public Page<CompanyListDTO> listUserSubscribedCompanies(String userId, Pageable pageable) {
        Page<Company> companies = repository.findByPointsAccountsUserId(userId, pageable);
        if(companies.isEmpty()) {
            return Page.empty();
        }
        return companies.map(mapper::toListDTO);
    }

    @Override
    @Transactional
    public CompanyListDTO setAppAdminOwner(AppAdminOwnerDTO dto){
        Company company = repository.findById(dto.companyId()).orElseThrow(()-> new NotFoundException("Company Not Found"));
        company.setAppAdminOwner(dto.appAdminOwner());
        return mapper.toListDTO(company);
    }
}

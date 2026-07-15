package com.tech.point_system.service.impl;

import com.tech.point_system.dto.company.CompanyDetailDTO;
import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.company.CompanyRequestDTO;
import com.tech.point_system.dto.company.CompanyUpdateDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.CompanyMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.security.user.model.User;
import com.tech.point_system.security.user.repository.UserRepository;
import com.tech.point_system.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository repository;
    private final CompanyMapper mapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CompanyDetailDTO addCompany(String companyAdminId, CompanyRequestDTO dto) {
        Company company = mapper.toEntity(dto);
        User proxyAdmin = userRepository.getReferenceById(companyAdminId);
        company.setAdmin(proxyAdmin);
        company = repository.save(company);
        return mapper.toDetailDTO(company);
    }

    @Override
    @Transactional
    public CompanyDetailDTO updateCompany(String companyAdminId, Long companyId, CompanyUpdateDTO dto) {
        Company company = repository.findById(companyId).orElseThrow(() -> new NotFoundException("Company Not Found"));
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
    public void deleteCompany(Long id) {
        repository.deleteById(id);
    }
}

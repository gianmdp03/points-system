package com.tech.point_system.service.impl;

import com.tech.point_system.dto.company.CompanyDetailDTO;
import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.company.CompanyRequestDTO;
import com.tech.point_system.dto.company.CompanyUpdateDTO;
import com.tech.point_system.mapper.CompanyMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository repository;
    private final CompanyMapper mapper;

    @Override
    @Transactional
    public CompanyDetailDTO addCompany(CompanyRequestDTO dto) {
        Company company = mapper.toEntity(dto);

    }

    @Override
    public CompanyDetailDTO updateCompany(Long id, CompanyUpdateDTO dto) {
        return null;
    }

    @Override
    public Page<CompanyListDTO> listCompanies(Pageable pageable) {
        return null;
    }

    @Override
    public CompanyDetailDTO getCompany(Long id) {
        return null;
    }

    @Override
    public void deleteCompany(Long id) {

    }
}

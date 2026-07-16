package com.tech.point_system.controller;

import com.tech.point_system.dto.company.CompanyDetailDTO;
import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.company.CompanyRequestDTO;
import com.tech.point_system.dto.company.CompanyUpdateDTO;
import com.tech.point_system.security.user._enum.Role;
import com.tech.point_system.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    @PreAuthorize("hasRole('APP_ADMIN')")
    @PostMapping
    public ResponseEntity<CompanyDetailDTO> addCompany(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CompanyRequestDTO dto)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.addCompany(jwt.getSubject(), dto));
    }

    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','APP_ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<CompanyDetailDTO> updateCompany(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody CompanyUpdateDTO dto){
        return ResponseEntity.status(HttpStatus.OK).body(companyService.updateCompany(jwt.getSubject(), id, dto));
    }

    @PreAuthorize("hasRole('APP_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<CompanyListDTO>> listCompanies(@PageableDefault(page = 0, size = 18, sort = "name", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(companyService.listCompanies(pageable));
    }

    @PreAuthorize("hasRole('APP_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<CompanyDetailDTO> getCompanyById(@PathVariable Long id){
        return ResponseEntity.status(HttpStatus.OK).body(companyService.getCompanyById(id));
    }


}

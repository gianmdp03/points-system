package com.tech.point_system.controller;

import com.tech.point_system.dto.company.CompanyNameDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.model.Company;
import com.tech.point_system.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/companies")
@RequiredArgsConstructor
public class CompanyPublicController {

    private final CompanyRepository companyRepository;

    @GetMapping("/{id}/name")
    public ResponseEntity<CompanyNameDTO> getCompanyName(@PathVariable Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comercio no encontrado."));

        if (!Boolean.TRUE.equals(company.getIsEnabled())) {
            throw new NotFoundException("El comercio no se encuentra disponible.");
        }

        return ResponseEntity.ok(new CompanyNameDTO(company.getId(), company.getName()));
    }
}

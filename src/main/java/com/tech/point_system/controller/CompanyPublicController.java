package com.tech.point_system.controller;

import com.tech.point_system.dto.company.CompanyNameDTO;
import com.tech.point_system.service.PublicCatalogService;
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

    private final PublicCatalogService publicCatalogService;

    @GetMapping("/{id}/name")
    public ResponseEntity<CompanyNameDTO> getCompanyName(@PathVariable Long id) {
        return ResponseEntity.ok(publicCatalogService.getPublicCompanyName(id));
    }
}

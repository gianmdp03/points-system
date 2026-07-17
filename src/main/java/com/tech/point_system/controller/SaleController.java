package com.tech.point_system.controller;

import com.tech.point_system.dto.sale.SaleDetailDTO;
import com.tech.point_system.dto.sale.SaleListDTO;
import com.tech.point_system.dto.sale.SaleRequestDTO;

import com.tech.point_system.service.SaleService;
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
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class SaleController {
    private final SaleService saleService;

    @PostMapping
    public ResponseEntity<SaleDetailDTO> addSale(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SaleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.addSale(jwt.getSubject(), dto));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<Page<SaleListDTO>> listCompaniesSales(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PageableDefault(page = 0, size = 18, sort = "amount", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(saleService.listCompaniesSales(jwt.getSubject(), companyId, pageable));
    }

    @GetMapping("/{companyId}/{id}")
    public ResponseEntity<SaleDetailDTO> getSaleById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(saleService.getSaleById(jwt.getSubject(), companyId, id));
    }
}

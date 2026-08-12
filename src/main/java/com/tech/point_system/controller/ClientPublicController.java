package com.tech.point_system.controller;

import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.model.Client;
import com.tech.point_system.repository.ClientRepository;
import com.tech.point_system.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/clients")
@RequiredArgsConstructor
public class ClientPublicController {

    private final ClientRepository clientRepository;
    private final CompanyService companyService;

    @GetMapping("/{country}/{dni}/companies")
    public ResponseEntity<Page<CompanyListDTO>> getClientCompanies(
            @PathVariable String country,
            @PathVariable String dni,
            @PageableDefault(page = 0, size = 18, sort = "name", direction = Sort.Direction.DESC) Pageable pageable) {

        // Buscamos si el cliente existe
        Client client = clientRepository.findByDniAndCountry(dni, country).orElse(null);

        if (client == null) {
            return ResponseEntity.ok(Page.empty());
        }

        // Devolvemos las empresas donde tiene puntos
        return ResponseEntity.ok(companyService.listClientSubscribedCompanies(client.getId(), pageable));
    }
}
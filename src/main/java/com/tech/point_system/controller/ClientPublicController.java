package com.tech.point_system.controller;

import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.company.CompanyPublicDetailDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.ProductMapper;
import com.tech.point_system.mapper.PromotionMapper;
import com.tech.point_system.mapper.RewardMapper;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.repository.ClientRepository;
import com.tech.point_system.repository.PointsAccountRepository;
import com.tech.point_system.repository.ProductRepository;
import com.tech.point_system.repository.PromotionRepository;
import com.tech.point_system.repository.RewardRepository;
import com.tech.point_system.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/public/clients")
@RequiredArgsConstructor
public class ClientPublicController {

    private final ClientRepository clientRepository;
    private final CompanyService companyService;
    private final PointsAccountRepository pointsAccountRepository;
    private final ProductRepository productRepository;
    private final RewardRepository rewardRepository;
    private final PromotionRepository promotionRepository;
    private final ProductMapper productMapper;
    private final RewardMapper rewardMapper;
    private final PromotionMapper promotionMapper;

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

    @GetMapping("/{country}/{dni}/companies/{companyId}")
    public ResponseEntity<CompanyPublicDetailDTO> getCompanyPublicDetail(
            @PathVariable String country,
            @PathVariable String dni,
            @PathVariable Long companyId) {

        // 1. Buscamos si el cliente existe
        Client client = clientRepository.findByDniAndCountry(dni, country)
                .orElseThrow(() -> new NotFoundException("No se encontró ningún cliente registrado con el DNI y País especificados."));

        // 2. Validamos que el cliente esté asociado a este comercio (posee PointsAccount)
        PointsAccount pointsAccount = pointsAccountRepository.findByClientIdAndCompanyId(client.getId(), companyId)
                .orElseThrow(() -> new NotFoundException("No estás asociado a este comercio."));

        Company company = pointsAccount.getCompany();
        if (company == null || !Boolean.TRUE.equals(company.getIsEnabled())) {
            throw new NotFoundException("El comercio no se encuentra disponible.");
        }

        // 3. Productos (Catálogo público de la empresa)
        List<ProductListDTO> productDTOs = productRepository.findByCompanyId(companyId).stream()
                .map(productMapper::toListDTO)
                .toList();

        // 4. Premios y Recompensas (Solo activos)
        List<RewardListDTO> rewardDTOs = rewardRepository.findByCompanyIdAndIsEnabledTrue(companyId).stream()
                .map(rewardMapper::toListDTO)
                .toList();

        // 5. Promociones ACTIVAS ÚNICAMENTE (startDate <= now <= endDate y isEnabled == true, no por venir ni vencidas)
        OffsetDateTime now = OffsetDateTime.now();
        List<PromotionListDTO> promotionDTOs = promotionRepository.findActivePromotions(companyId, now).stream()
                .map(promotionMapper::toListDTO)
                .toList();

        // 6. Armamos el DTO de respuesta pública
        CompanyPublicDetailDTO response = new CompanyPublicDetailDTO(
                company.getId(),
                company.getName(),
                company.getCompanyDetails(),
                company.getAmountStep(),
                company.getPointsPerStep(),
                company.getIsEnabled(),
                pointsAccount.getBalance(),
                client.getName(),
                productDTOs,
                promotionDTOs,
                rewardDTOs
        );

        return ResponseEntity.ok(response);
    }
}

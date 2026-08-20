package com.tech.point_system.controller;

import com.tech.point_system.dto.client.ClientJoinRequestDTO;
import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.company.CompanyPublicDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.PointsAccountMapper;
import com.tech.point_system.mapper.PromotionMapper;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.repository.ClientRepository;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.PointsAccountRepository;
import com.tech.point_system.repository.PromotionRepository;
import com.tech.point_system.service.CompanyService;
import com.tech.point_system.service.PlanValidatorService;
import com.tech.point_system.service.PublicCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/public/clients")
@RequiredArgsConstructor
public class ClientPublicController {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final PointsAccountRepository pointsAccountRepository;
    private final PointsAccountMapper pointsAccountMapper;
    private final PublicCatalogService publicCatalogService;
    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final PlanValidatorService planValidatorService;

    @PostMapping("/join")
    @Transactional
    public ResponseEntity<PointsAccountDetailDTO> joinCompany(@Valid @RequestBody ClientJoinRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new NotFoundException("Comercio no encontrado."));

        if (!Boolean.TRUE.equals(company.getIsEnabled())) {
            throw new ConflictException("Este comercio se encuentra temporalmente inactivo.");
        }

        long currentClients = pointsAccountRepository.countByCompanyId(company.getId());
        planValidatorService.validateClientCreation(company.getAdmin().getId(), (int) currentClients);

        Client client = clientRepository.getOrCreateClient(
                dto.dni(), dto.country(), dto.name(), dto.email(), dto.phone());

        pointsAccountRepository.findByClientIdAndCompanyId(client.getId(), company.getId())
                .ifPresent(existingAccount -> {
                    throw new ConflictException("¡Ya estás registrado en esta sucursal!");
                });

        PointsAccount newAccount = new PointsAccount();
        newAccount.setClient(client);
        newAccount.setCompany(company);
        newAccount.setBalance(0);
        newAccount.setLastActivityDate(OffsetDateTime.now(ZoneOffset.UTC));

        PointsAccount savedAccount = pointsAccountRepository.save(newAccount);
        return ResponseEntity.status(HttpStatus.CREATED).body(pointsAccountMapper.toDetailDTO(savedAccount));
    }

    @GetMapping("/{country}/{dni}/companies")
    public ResponseEntity<Page<CompanyListDTO>> getClientCompanies(
            @PathVariable String country,
            @PathVariable String dni,
            @PageableDefault(page = 0, size = 18, sort = "name", direction = Sort.Direction.DESC) Pageable pageable) {

        Client client = clientRepository.findByDniAndCountry(dni, country).orElse(null);

        if (client == null) {
            return ResponseEntity.ok(Page.empty());
        }

        return ResponseEntity.ok(companyService.listClientSubscribedCompanies(client.getId(), pageable));
    }

    @GetMapping("/{country}/{dni}/companies/{companyId}")
    public ResponseEntity<CompanyPublicDetailDTO> getCompanyPublicDetail(
            @PathVariable String country,
            @PathVariable String dni,
            @PathVariable Long companyId) {

        Client client = clientRepository.findByDniAndCountry(dni, country)
                .orElseThrow(() -> new NotFoundException("No se encontró ningún cliente registrado con el DNI y País especificados."));

        PointsAccount pointsAccount = pointsAccountRepository.findByClientIdAndCompanyId(client.getId(), companyId)
                .orElseThrow(() -> new NotFoundException("No estás asociado a este comercio."));

        Company company = pointsAccount.getCompany();
        if (company == null || !Boolean.TRUE.equals(company.getIsEnabled())) {
            throw new NotFoundException("El comercio no se encuentra disponible.");
        }

        List<ProductListDTO> productDTOs = publicCatalogService.getPublicProducts(companyId);
        List<RewardListDTO> rewardDTOs = publicCatalogService.getPublicRewards(companyId);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<PromotionListDTO> promotionDTOs = promotionRepository.findActivePromotions(companyId, now).stream()
                .map(promotionMapper::toListDTO)
                .toList();

        CompanyPublicDetailDTO response = new CompanyPublicDetailDTO(
                company.getId(),
                company.getName(),
                company.getCompanyDetails(),
                company.getAmountStep(),
                company.getPointsPerStep(),
                company.getIsEnabled(),
                pointsAccount.getBalance(),
                client.getName(),
                company.getIsPointsExpirationEnabled(),
                company.getPointsExpirationDays(),
                company.getIsInactiveClientPurgeEnabled(),
                company.getInactiveClientPurgeDays(),
                productDTOs,
                promotionDTOs,
                rewardDTOs
        );

        return ResponseEntity.ok(response);
    }
}

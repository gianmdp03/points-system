package com.tech.point_system.service;

import com.tech.point_system.dto.company.CompanyNameDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.ProductMapper;
import com.tech.point_system.mapper.PromotionMapper;
import com.tech.point_system.mapper.RewardMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.ProductRepository;
import com.tech.point_system.repository.PromotionRepository;
import com.tech.point_system.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCatalogService {

    private final ProductRepository productRepository;
    private final RewardRepository rewardRepository;
    private final PromotionRepository promotionRepository;
    private final CompanyRepository companyRepository;
    private final ProductMapper productMapper;
    private final RewardMapper rewardMapper;
    private final PromotionMapper promotionMapper;

    @Cacheable(value = "public_company_products", key = "#companyId")
    public List<ProductListDTO> getPublicProducts(Long companyId) {
        return productRepository.findByCompanyId(companyId).stream()
                .map(productMapper::toListDTO)
                .toList();
    }

    @Cacheable(value = "public_company_rewards", key = "#companyId")
    public List<RewardListDTO> getPublicRewards(Long companyId) {
        return rewardRepository.findByCompanyIdAndIsEnabledTrue(companyId).stream()
                .map(rewardMapper::toListDTO)
                .toList();
    }

    @Cacheable(value = "company_active_promotions", key = "#companyId")
    public List<PromotionListDTO> getPublicPromotions(Long companyId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return promotionRepository.findActivePromotions(companyId, now).stream()
                .map(promotionMapper::toListDTO)
                .toList();
    }

    @Cacheable(value = "public_company_name", key = "#companyId")
    public CompanyNameDTO getPublicCompanyName(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Comercio no encontrado."));

        if (!Boolean.TRUE.equals(company.getIsEnabled())) {
            throw new NotFoundException("El comercio no se encuentra disponible.");
        }

        return new CompanyNameDTO(company.getId(), company.getName());
    }
}

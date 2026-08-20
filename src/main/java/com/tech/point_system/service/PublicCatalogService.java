package com.tech.point_system.service;

import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.mapper.ProductMapper;
import com.tech.point_system.mapper.RewardMapper;
import com.tech.point_system.repository.ProductRepository;
import com.tech.point_system.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCatalogService {

    private final ProductRepository productRepository;
    private final RewardRepository rewardRepository;
    private final ProductMapper productMapper;
    private final RewardMapper rewardMapper;

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
}

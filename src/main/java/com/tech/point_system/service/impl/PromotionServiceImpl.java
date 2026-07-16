package com.tech.point_system.service.impl;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.promotion.PromotionRequestDTO;
import com.tech.point_system.dto.promotion.PromotionUpdateDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.PromotionMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.Promotion;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.PromotionRepository;
import com.tech.point_system.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final CompanyRepository companyRepository;
    private final PromotionMapper promotionMapper;

    @Override
    @Transactional
    public PromotionDetailDTO addPromotion(PromotionRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new NotFoundException("Company not found"));

        Promotion promotion = promotionMapper.toEntity(dto);

        promotion.setCompany(company);

        Promotion savedPromotion = promotionRepository.save(promotion);

        return promotionMapper.toDetailDTO(savedPromotion);
    }

    @Override
    @Transactional
    public PromotionDetailDTO updatePromotion(Long id, PromotionUpdateDTO dto) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found"));

        promotionMapper.updateEntityFromDTO(dto, promotion);

        Promotion updatedPromotion = promotionRepository.save(promotion);

        return promotionMapper.toDetailDTO(updatedPromotion);
    }

    @Override
    public Page<PromotionListDTO> listPromotions(Long companyId, Pageable pageable){
        Page<Promotion> promotions = promotionRepository.findByCompanyId(companyId, pageable);
        if(promotions.isEmpty()){
            return Page.empty();
        }
        return promotions.map(promotionMapper::toListDTO);
    }

    @Override
    public PromotionDetailDTO getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found"));

        return promotionMapper.toDetailDTO(promotion);
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new NotFoundException("Promotion not found");
        }
        promotionRepository.deleteById(id);
    }
}
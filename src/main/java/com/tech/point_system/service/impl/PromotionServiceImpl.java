package com.tech.point_system.service.impl;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
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
    public PromotionDetailDTO addPromotion(PromotionRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new NotFoundException("El comercio no existe."));

        Promotion promotion = promotionMapper.toEntity(dto);

        promotion.setCompany(company);

        Promotion savedPromotion = promotionRepository.save(promotion);

        return promotionMapper.toDetailDTO(savedPromotion);
    }

    @Override
    public PromotionDetailDTO updatePromotion(Long id, PromotionUpdateDTO dto) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promoción no encontrada."));

        promotionMapper.updateEntityFromDTO(dto, promotion);

        Promotion updatedPromotion = promotionRepository.save(promotion);

        return promotionMapper.toDetailDTO(updatedPromotion);
    }

    @Override
    public PromotionDetailDTO getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promoción no encontrada."));

        return promotionMapper.toDetailDTO(promotion);
    }

    @Override
    public void deletePromotion(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new NotFoundException("Promoción no encontrada.");
        }
        promotionRepository.deleteById(id);
    }
}
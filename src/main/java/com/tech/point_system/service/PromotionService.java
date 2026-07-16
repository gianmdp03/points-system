package com.tech.point_system.service;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.promotion.PromotionRequestDTO;
import com.tech.point_system.dto.promotion.PromotionUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PromotionService {
    PromotionDetailDTO addPromotion(PromotionRequestDTO dto);
    PromotionDetailDTO updatePromotion(Long id, PromotionUpdateDTO dto);
    Page<PromotionListDTO> listPromotions(Long companyId, Pageable pageable);
    PromotionDetailDTO getPromotionById(Long id);
    void deletePromotion(Long id);
}

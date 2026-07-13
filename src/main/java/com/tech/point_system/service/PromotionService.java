package com.tech.point_system.service;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.dto.promotion.PromotionRequestDTO;
import com.tech.point_system.dto.promotion.PromotionUpdateDTO;

public interface PromotionService {
    PromotionDetailDTO addPromotion(PromotionRequestDTO dto);
    PromotionDetailDTO updatePromotion(Long id, PromotionUpdateDTO dto);
    PromotionDetailDTO getPromotion(Long id);
    void deletePromotion(Long id);
}

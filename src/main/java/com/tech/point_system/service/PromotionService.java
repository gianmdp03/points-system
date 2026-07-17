package com.tech.point_system.service;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.promotion.PromotionRequestDTO;
import com.tech.point_system.dto.promotion.PromotionUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PromotionService {
    PromotionDetailDTO addPromotion(String companyAdminId, PromotionRequestDTO dto);
    PromotionDetailDTO updatePromotion(String companyAdminId, Long companyId, Long id, PromotionUpdateDTO dto);
    Page<PromotionListDTO> listPromotions(String companyAdminId, Long companyId, Pageable pageable);
    PromotionDetailDTO getPromotionById(String companyAdminId, Long companyId, Long id);
    void deletePromotion(String companyAdminId, Long companyId, Long id);
}

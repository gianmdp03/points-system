package com.tech.point_system.dto.company;

import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.extra.CompanyDetails;

import java.math.BigDecimal;
import java.util.List;

public record CompanyPublicDetailDTO(
        Long id,
        String name,
        CompanyDetails companyDetails,
        BigDecimal amountStep,
        Integer pointsPerStep,
        Boolean isEnabled,
        Integer clientBalance,
        String clientName,
        Boolean isNotificationEnabled,
        Boolean isPointsExpirationEnabled,
        Integer pointsExpirationDays,
        Boolean isInactiveClientPurgeEnabled,
        Integer inactiveClientPurgeDays,
        List<ProductListDTO> products,
        List<PromotionListDTO> activePromotions,
        List<RewardListDTO> rewards
) {
}

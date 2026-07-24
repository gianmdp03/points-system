package com.tech.point_system.dto.company;

import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.extra.CompanyDetails;

import java.math.BigDecimal;
import java.util.List;

public record CompanyDetailDTO(Long id, String name, CompanyDetails companyDetails, BigDecimal amountStep, Integer pointsPerStep, Boolean isEnabled, List<PointsAccountDetailDTO> pointsAccounts, List<ProductListDTO> products, List<PromotionListDTO> promotions, List<RewardListDTO> rewards) {
}

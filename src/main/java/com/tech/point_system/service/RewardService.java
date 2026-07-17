package com.tech.point_system.service;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.dto.promotion.PromotionRequestDTO;
import com.tech.point_system.dto.reward.RewardDetailDTO;
import com.tech.point_system.dto.reward.RewardRequestDTO;
import com.tech.point_system.dto.reward.RewardUpdateDTO;

public interface RewardService {
    RewardDetailDTO addReward(String companyAdminId, RewardRequestDTO dto);
    RewardDetailDTO updateReward(String companyAdminId, Long id, RewardUpdateDTO dto);
    RewardDetailDTO getRewardById(String companyAdminId, Long id);
    void deleteReward(String companyAdminId, Long id);
}

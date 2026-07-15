package com.tech.point_system.service;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.dto.promotion.PromotionRequestDTO;
import com.tech.point_system.dto.reward.RewardDetailDTO;
import com.tech.point_system.dto.reward.RewardRequestDTO;
import com.tech.point_system.dto.reward.RewardUpdateDTO;

public interface RewardService {
    RewardDetailDTO addReward(RewardRequestDTO dto);
    RewardDetailDTO updateReward(Long id, RewardUpdateDTO dto);
    RewardDetailDTO getRewardById(Long id);
    void deleteReward(Long id);
}

package com.tech.point_system.service;


import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.reward.RewardDetailDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.dto.reward.RewardRequestDTO;
import com.tech.point_system.dto.reward.RewardUpdateDTO;
import com.tech.point_system.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RewardService {
    RewardDetailDTO addReward(RewardRequestDTO dto);
    RewardDetailDTO updateReward(Long id, RewardUpdateDTO dto);
    Page<RewardListDTO> listRewards(Long companyId, Pageable pageable);
    RewardDetailDTO getRewardById(Long id);
    void deleteReward(Long id);
}

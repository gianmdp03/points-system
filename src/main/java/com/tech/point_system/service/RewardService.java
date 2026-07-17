package com.tech.point_system.service;

import com.tech.point_system.dto.reward.RewardDetailDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.dto.reward.RewardRequestDTO;
import com.tech.point_system.dto.reward.RewardUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RewardService {
    RewardDetailDTO addReward(String companyAdminId, RewardRequestDTO dto);
    RewardDetailDTO updateReward(String companyAdminId, Long companyId, Long id, RewardUpdateDTO dto);
    Page<RewardListDTO> listRewards(String companyAdminId, Long companyId, Pageable pageable);
    RewardDetailDTO getRewardById(String companyAdminId, Long companyId, Long id);
    void deleteReward(String companyAdminId, Long companyId, Long id);
}

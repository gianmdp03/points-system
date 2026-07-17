package com.tech.point_system.service.impl;

import com.tech.point_system.dto.reward.RewardDetailDTO;
import com.tech.point_system.dto.reward.RewardRequestDTO;
import com.tech.point_system.dto.reward.RewardUpdateDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.RewardMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.Reward;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.RewardRepository;
import com.tech.point_system.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {
    private final RewardRepository rewardRepository;
    private final CompanyRepository companyRepository;
    private final RewardMapper rewardMapper;

    @Override
    @Transactional
    public RewardDetailDTO addReward(String companyAdminId, RewardRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new NotFoundException("Company Not Found"));

        Reward reward = rewardMapper.toEntity(dto);

        reward.setCompany(company);

        Reward savedReward = rewardRepository.save(reward);

        return rewardMapper.toDetailDTO(savedReward);
    }

    @Override
    @Transactional
    public RewardDetailDTO updateReward(String companyAdminId, Long id, RewardUpdateDTO dto) {
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reward not found"));

        rewardMapper.updateEntityFromDTO(dto, reward);

        Reward updatedReward = rewardRepository.save(reward);

        return rewardMapper.toDetailDTO(updatedReward);
    }

    @Override
    public RewardDetailDTO getRewardById(String companyAdminId, Long id) {
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reward not found"));

        return rewardMapper.toDetailDTO(reward);
    }

    @Override
    @Transactional
    public void deleteReward(String companyAdminId, Long id) {
        if (!rewardRepository.existsById(id)) {
            throw new NotFoundException("Reward not found");
        }
        rewardRepository.deleteById(id);
    }
}

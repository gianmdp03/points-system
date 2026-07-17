package com.tech.point_system.service.impl;

import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.reward.RewardDetailDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.dto.reward.RewardRequestDTO;
import com.tech.point_system.dto.reward.RewardUpdateDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.RewardMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.Product;
import com.tech.point_system.model.Reward;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.RewardRepository;
import com.tech.point_system.security.user.service.CompanyAccessValidator;
import com.tech.point_system.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {
    private final RewardRepository rewardRepository;
    private final CompanyRepository companyRepository;
    private final RewardMapper rewardMapper;
    private final CompanyAccessValidator companyAccessValidator;

    @Override
    @Transactional
    public RewardDetailDTO addReward(String companyAdminId, RewardRequestDTO dto) {
        Company company = companyAccessValidator.validateAccess(dto.companyId(), companyAdminId);

        Reward reward = rewardMapper.toEntity(dto);

        reward.setCompany(company);

        Reward savedReward = rewardRepository.save(reward);

        return rewardMapper.toDetailDTO(savedReward);
    }

    @Override
    @Transactional
    public RewardDetailDTO updateReward(String companyAdminId, Long companyId, Long id, RewardUpdateDTO dto) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reward not found"));

        rewardMapper.updateEntityFromDTO(dto, reward);

        Reward updatedReward = rewardRepository.save(reward);

        return rewardMapper.toDetailDTO(updatedReward);
    }

    @Override
    public Page<RewardListDTO> listRewards(String companyAdminId, Long companyId, Pageable pageable) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
        Page<Reward> rewards = rewardRepository.findByCompanyId(companyId, pageable);

        if(rewards.isEmpty()) {
            return Page.empty();
        }

        return rewards.map(rewardMapper::toListDTO);
    }

    @Override
    public RewardDetailDTO getRewardById(String companyAdminId, Long companyId, Long id) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reward not found"));

        return rewardMapper.toDetailDTO(reward);
    }

    @Override
    @Transactional
    public void deleteReward(String companyAdminId, Long companyId, Long id) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
        if (!rewardRepository.existsById(id)) {
            throw new NotFoundException("Reward not found");
        }
        rewardRepository.deleteById(id);
    }
}

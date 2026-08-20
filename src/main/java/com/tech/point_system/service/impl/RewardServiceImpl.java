package com.tech.point_system.service.impl;

import com.tech.point_system.dto.reward.RewardDetailDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.dto.reward.RewardRequestDTO;
import com.tech.point_system.dto.reward.RewardUpdateDTO;
import com.tech.point_system.event.RewardRedeemEvent;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.RewardMapper;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.Reward;
import com.tech.point_system.repository.ClientRepository;
import com.tech.point_system.repository.RewardRepository;
import com.tech.point_system.service.CompanyAccessValidator;
import com.tech.point_system.service.PlanValidatorService;
import com.tech.point_system.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {
    private final RewardRepository rewardRepository;
    private final ClientRepository clientRepository;
    private final RewardMapper rewardMapper;
    private final CompanyAccessValidator companyAccessValidator;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PlanValidatorService planValidatorService;

    @Override
    @Transactional
    @CacheEvict(value = "public_company_rewards", key = "#dto.companyId()")
    public RewardDetailDTO addReward(String companyAdminId, RewardRequestDTO dto) {
        Company company = companyAccessValidator.validateAccess(dto.companyId(), companyAdminId);

        long currentRewards = rewardRepository.countByCompanyId(company.getId());
        planValidatorService.validateRewardCreation(companyAdminId, (int) currentRewards);

        Reward reward = rewardMapper.toEntity(dto);
        reward.setCompany(company);

        Reward savedReward = rewardRepository.save(reward);
        return rewardMapper.toDetailDTO(savedReward);
    }

    @Override
    @Transactional
    public void redeemReward(String companyAdminId, Long companyId, Long id, String dni, String country) {
        Company company = companyAccessValidator.validateAccess(companyId, companyAdminId);

        Client client = clientRepository.findByDniAndCountry(dni, country)
                .orElseThrow(() -> new NotFoundException("Client not found"));

        Reward reward = rewardRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Reward not found"));

        applicationEventPublisher.publishEvent(new RewardRedeemEvent(reward.getCostInPoints(), company, client));
    }

    @Override
    @Transactional
    @CacheEvict(value = "public_company_rewards", key = "#companyId")
    public RewardDetailDTO updateReward(String companyAdminId, Long companyId, Long id, RewardUpdateDTO dto) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
        Reward reward = rewardRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Reward not found"));

        rewardMapper.updateEntityFromDTO(dto, reward);
        Reward updatedReward = rewardRepository.save(reward);
        return rewardMapper.toDetailDTO(updatedReward);
    }

    @Override
    public Page<RewardListDTO> listRewards(String companyAdminId, Long companyId, Pageable pageable) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
        Page<Reward> rewards = rewardRepository.findByCompanyId(companyId, pageable);
        if (rewards.isEmpty()) {
            return Page.empty();
        }
        return rewards.map(rewardMapper::toListDTO);
    }

    @Override
    public RewardDetailDTO getRewardById(String companyAdminId, Long companyId, Long id) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
        Reward reward = rewardRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Reward not found"));
        return rewardMapper.toDetailDTO(reward);
    }

    @Override
    @Transactional
    @CacheEvict(value = "public_company_rewards", key = "#companyId")
    public void enableOrDisableReward(String companyAdminId, Long companyId, Long id) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
        Reward reward = rewardRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Reward not found"));
        reward.setIsEnabled(!reward.getIsEnabled());
        rewardRepository.save(reward);
    }
}

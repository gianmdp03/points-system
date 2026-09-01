package com.tech.point_system.service.impl;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.promotion.PromotionRequestDTO;
import com.tech.point_system.dto.promotion.PromotionUpdateDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.PromotionMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.Promotion;
import com.tech.point_system.repository.PromotionRepository;
import com.tech.point_system.service.CompanyAccessValidator;
import com.tech.point_system.service.PlanValidatorService;
import com.tech.point_system.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.repository.PointsAccountRepository;
import com.tech.point_system.service.EmailService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

  private final PromotionRepository promotionRepository;
  private final PromotionMapper promotionMapper;
  private final CompanyAccessValidator companyAccessValidator;
  private final PlanValidatorService planValidatorService;
  private final PointsAccountRepository pointsAccountRepository;
  private final EmailService emailService;

  @Override
  @Transactional
  @CacheEvict(value = "company_active_promotions", key = "#dto.companyId()")
  public PromotionDetailDTO addPromotion(String companyAdminId, PromotionRequestDTO dto) {
    Company company = companyAccessValidator.validateAccess(dto.companyId(), companyAdminId);

    planValidatorService.validatePromotionCreation(companyAdminId);

    Promotion promotion = promotionMapper.toEntity(dto);
    promotion.setCompany(company);

    Promotion savedPromotion = promotionRepository.save(promotion);

    if (Boolean.TRUE.equals(savedPromotion.getIsEnabled())) {
        broadcastPromotionNotification(savedPromotion, company);
    }

    return promotionMapper.toDetailDTO(savedPromotion);
  }

  @Override
  @Transactional
  @CacheEvict(value = "company_active_promotions", key = "#companyId")
  public PromotionDetailDTO updatePromotion(String companyAdminId, Long companyId, Long id, PromotionUpdateDTO dto) {
    Company company = companyAccessValidator.validateAccess(companyId, companyAdminId);
    Promotion promotion = promotionRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new NotFoundException("Promotion not found"));

    boolean wasEnabled = Boolean.TRUE.equals(promotion.getIsEnabled());
    promotionMapper.updateEntityFromDTO(dto, promotion);
    Promotion updatedPromotion = promotionRepository.save(promotion);

    if (!wasEnabled && Boolean.TRUE.equals(updatedPromotion.getIsEnabled())) {
        broadcastPromotionNotification(updatedPromotion, company);
    }

    return promotionMapper.toDetailDTO(updatedPromotion);
  }

  @Override
  public Page<PromotionListDTO> listPromotions(String companyAdminId, Long companyId, Pageable pageable) {
    companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
    Page<Promotion> promotions = promotionRepository.findByCompanyId(companyId, pageable);
    if (promotions.isEmpty()) {
      return Page.empty();
    }
    return promotions.map(promotionMapper::toListDTO);
  }

  @Override
  public PromotionDetailDTO getPromotionById(String companyAdminId, Long companyId, Long id) {
    companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
    Promotion promotion = promotionRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new NotFoundException("Promotion not found"));
    return promotionMapper.toDetailDTO(promotion);
  }

  @Override
  @Transactional
  @CacheEvict(value = "company_active_promotions", key = "#companyId")
  public void enabledOrDisabled(String companyAdminId, Long companyId, Long id) {
    Company company = companyAccessValidator.validateAccess(companyId, companyAdminId);
    Promotion promotion = promotionRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new NotFoundException("Promotion not found"));
    promotion.setIsEnabled(!promotion.getIsEnabled());
    Promotion saved = promotionRepository.save(promotion);

    if (Boolean.TRUE.equals(saved.getIsEnabled())) {
        broadcastPromotionNotification(saved, company);
    }
  }

  private void broadcastPromotionNotification(Promotion promotion, Company company) {
    List<PointsAccount> clientAccounts = pointsAccountRepository.findClientsForPromotionBroadcast(company.getId());
    if (clientAccounts.isEmpty()) {
        return;
    }

    String multiplierStr = (promotion.getMultiplier() != null) ? promotion.getMultiplier().toString() + "x" : "2x";
    Map<String, Object> extraParams = new HashMap<>();
    extraParams.put("promotionName", promotion.getName());
    extraParams.put("promoName", promotion.getName());
    extraParams.put("promotionDescription", promotion.getDescription() != null ? promotion.getDescription() : "");
    extraParams.put("multiplier", multiplierStr);
    extraParams.put("localName", company.getName());
    extraParams.put("local", company.getName());
    extraParams.put("empresa", company.getName());

    for (PointsAccount account : clientAccounts) {
        Client client = account.getClient();
        if (client != null && Boolean.TRUE.equals(client.getIsNotificationEnabled()) && client.getEmail() != null && !client.getEmail().isBlank()) {
            emailService.sendNotificationEmail(
                    NotificationType.PROMOTION_NOTIFICATION,
                    company,
                    client,
                    extraParams
            );
        }
    }
  }
}

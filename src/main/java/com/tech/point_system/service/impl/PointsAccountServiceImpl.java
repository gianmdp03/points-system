package com.tech.point_system.service.impl;

import com.tech.point_system._enum.TransactionType;
import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import com.tech.point_system.dto.pointsTransaction.PointsTransactionDetailDTO;
import com.tech.point_system.event.RewardRedeemEvent;
import com.tech.point_system.event.SaleCreatedEvent;
import com.tech.point_system.exception.BadRequestException;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.PointsAccountMapper;
import com.tech.point_system.mapper.PointsTransactionMapper;
import com.tech.point_system.model.*;
import com.tech.point_system.repository.*;
import com.tech.point_system.service.CompanyAccessValidator;
import com.tech.point_system.service.PlanValidatorService;
import com.tech.point_system.service.PointsAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointsAccountServiceImpl implements PointsAccountService {
  private final PointsAccountRepository pointsAccountRepository;
  private final ClientRepository clientRepository;
  private final PointsAccountMapper pointsAccountMapper;
  private final PointsTransactionRepository transactionRepository;
  private final PointsTransactionMapper transactionMapper;
  private final CompanyAccessValidator companyAccessValidator;
  private final PlanValidatorService planValidatorService;
  private final PromotionRepository promotionRepository;

  @Override
  @Transactional
  public PointsAccountDetailDTO registerClientAndCreateAccount(String companyAdminId, PointsAccountRequestDTO dto) {
    Company company = companyAccessValidator.validateAccess(dto.companyId(), companyAdminId);

    long currentClients = pointsAccountRepository.countByCompanyId(company.getId());
    planValidatorService.validateClientCreation(companyAdminId, (int) currentClients);

    Client client = clientRepository.getOrCreateClient(
            dto.dni(), dto.country(), dto.name(), dto.email(), dto.phone(), dto.isNotificationEnabled());

    pointsAccountRepository.findByClientIdAndCompanyId(client.getId(), company.getId())
            .ifPresent(existingAccount -> {
              throw new ConflictException("El cliente ya se encuentra registrado y asociado a este comercio.");
            });

    PointsAccount newAccount = new PointsAccount();
    newAccount.setClient(client);
    newAccount.setCompany(company);
    newAccount.setBalance(0);
    OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
    newAccount.setLastActivityDate(nowUtc);

    PointsAccount savedAccount = pointsAccountRepository.save(newAccount);
    return pointsAccountMapper.toDetailDTO(savedAccount);
  }

  @Override
  public Page<PointsAccountDetailDTO> listPointsAccounts(String companyAdminId, Long companyId, Pageable pageable) {
    companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
    return pointsAccountRepository.findByCompanyId(companyId, pageable).map(pointsAccountMapper::toDetailDTO);
  }

  @Override
  public Page<PointsAccountDetailDTO> listInactiveClients(String companyAdminId, Long companyId, int days, Pageable pageable) {
    companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
    int safeDays = Math.max(1, days);
    OffsetDateTime thresholdDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(safeDays);
    return pointsAccountRepository.findInactiveAccounts(companyId, thresholdDate, pageable).map(pointsAccountMapper::toDetailDTO);
  }

  @Override
  public Page<PointsTransactionDetailDTO> getTransactionHistory(Long clientId, Long companyId, Pageable pageable) {
    PointsAccount pointsAccount = pointsAccountRepository.findByClientIdAndCompanyId(clientId, companyId)
            .orElseThrow(() -> new NotFoundException("Points account not found for client " + clientId + " in company " + companyId));
    return transactionRepository.findByPointsAccount(pointsAccount, pageable).map(transactionMapper::toDetailDTO);
  }

  @EventListener
  @Transactional
  public void handleSaleCreated(SaleCreatedEvent event) {
    PointsAccount pointsAccount = pointsAccountRepository
            .findByClientIdAndCompanyId(event.client().getId(), event.company().getId())
            .orElseGet(() -> {
              PointsAccount newAccount = new PointsAccount();
              newAccount.setClient(event.client());
              newAccount.setCompany(event.company());
              newAccount.setBalance(0);
              newAccount.setLastActivityDate(OffsetDateTime.now(ZoneOffset.UTC));
              return pointsAccountRepository.save(newAccount);
            });

    BigDecimal amount = event.amount();
    BigDecimal amountStep = event.company().getAmountStep();
    Integer pointsPerStep = event.company().getPointsPerStep();

    if (amountStep == null || amountStep.compareTo(BigDecimal.ZERO) <= 0) {
      amountStep = new BigDecimal("100");
    }
    if (pointsPerStep == null || pointsPerStep <= 0) {
      pointsPerStep = 1;
    }

    int steps = amount.divideToIntegralValue(amountStep).intValue();
    int basePoints = steps * pointsPerStep;

    if (basePoints <= 0) {
      return;
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Promotion activePromo = promotionRepository.findActivePromotion(event.company().getId(), now).orElse(null);
    BigDecimal multiplier = (activePromo != null && activePromo.getMultiplier() != null)
            ? activePromo.getMultiplier()
            : BigDecimal.ONE;

    int pointsToEarn = BigDecimal.valueOf(basePoints)
            .multiply(multiplier)
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();

    if (pointsToEarn <= 0) {
      return;
    }

    pointsAccount.setBalance(pointsAccount.getBalance() + pointsToEarn);
    pointsAccount.setLastActivityDate(now);
    pointsAccount = pointsAccountRepository.save(pointsAccount);

    PointsTransaction transaction = new PointsTransaction();
    transaction.setPointsAccount(pointsAccount);
    transaction.setAmount(pointsToEarn);
    transaction.setAvailableAmount(pointsToEarn);
    transaction.setTransactionType(TransactionType.EARNED);
    transaction.setCreatedAt(now);

    if (Boolean.TRUE.equals(event.company().getIsPointsExpirationEnabled()) && event.company().getPointsExpirationDays() != null) {
      transaction.setExpiresAt(now.plusDays(event.company().getPointsExpirationDays()));
    } else {
      transaction.setExpiresAt(null);
    }

    transactionRepository.save(transaction);

    log.info("Venta procesada con éxito. Se acreditaron {} puntos (Base: {}, Multiplicador: {}). Vencimiento: {}. Nuevo balance: {}",
            pointsToEarn, basePoints, multiplier, transaction.getExpiresAt(), pointsAccount.getBalance());
  }

  @EventListener
  @Transactional
  public void deductPoints(RewardRedeemEvent event) {
    PointsAccount account = pointsAccountRepository
            .findByClientIdAndCompanyId(event.client().getId(), event.company().getId())
            .orElseThrow(() -> new NotFoundException("Points account not found"));

    if (account.getBalance() < event.costInPoints()) {
      throw new BadRequestException("Insufficient balance in the points account");
    }

    // Logica FIFO en Batch: Buscar transacciones EARNED con availableAmount > 0 ordenadas por createdAt ASC
    int costRemaining = event.costInPoints();
    List<PointsTransaction> earnedTransactions = transactionRepository
            .findByPointsAccountIdAndTransactionTypeAndAvailableAmountGreaterThanOrderByCreatedAtAsc(
                    account.getId(), TransactionType.EARNED, 0);

    List<PointsTransaction> updatedEarnedTxs = new ArrayList<>();

    for (PointsTransaction earnedTx : earnedTransactions) {
        if (costRemaining <= 0) {
            break;
        }
        int available = earnedTx.getAvailableAmount() != null ? earnedTx.getAvailableAmount() : 0;
        if (available <= 0) {
            continue;
        }

        if (available >= costRemaining) {
            earnedTx.setAvailableAmount(available - costRemaining);
            costRemaining = 0;
        } else {
            costRemaining -= available;
            earnedTx.setAvailableAmount(0);
        }
        updatedEarnedTxs.add(earnedTx);
    }

    // Guardado por lote de transacciones modificadas
    if (!updatedEarnedTxs.isEmpty()) {
        transactionRepository.saveAll(updatedEarnedTxs);
    }

    account.setBalance(account.getBalance() - event.costInPoints());
    OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
    account.setLastActivityDate(nowUtc);
    account = pointsAccountRepository.save(account);

    PointsTransaction transaction = new PointsTransaction();
    transaction.setPointsAccount(account);
    transaction.setAmount(-event.costInPoints());
    transaction.setAvailableAmount(0);
    transaction.setTransactionType(TransactionType.REDEEMED);
    transaction.setCreatedAt(nowUtc);
    transaction.setExpiresAt(null);

    transactionRepository.save(transaction);

    log.info("Canje procesado con éxito (FIFO Batch). Se descontaron {} puntos. Nuevo balance: {}",
            event.costInPoints(), account.getBalance());
  }
}

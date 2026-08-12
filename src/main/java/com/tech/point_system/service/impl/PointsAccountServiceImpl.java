package com.tech.point_system.service.impl;

import com.tech.point_system._enum.TransactionType;
import com.tech.point_system.dto.client.ClientDetailDTO;
import com.tech.point_system.dto.company.CompanyListDTO;
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
import com.tech.point_system._enum.Role;
import com.tech.point_system.dto.user.UserDetailDTO;
import com.tech.point_system.service.CompanyAccessValidator;
import com.tech.point_system.service.SupabaseAdminClient;
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

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PointsAccountServiceImpl implements PointsAccountService {
  private final PointsAccountMapper mapper;
  private final ClientRepository clientRepository;
  private final PromotionRepository promotionRepository;
  private final PointsAccountRepository pointsAccountRepository;
  private final PointsTransactionRepository transactionRepository;
  private final CompanyAccessValidator companyAccessValidator;
  private final PointsTransactionMapper transactionMapper;

  @Override
  @Transactional
  public PointsAccountDetailDTO registerClientAndCreateAccount(String companyAdminId, PointsAccountRequestDTO dto) {
    Company company = companyAccessValidator.validateAccess(dto.companyId(), companyAdminId);

    // Usamos el Helper del Repo
    Client client = clientRepository.getOrCreateClient(dto.dni(), dto.country(), dto.name(), dto.email(), dto.phone());

    if (pointsAccountRepository.findByClientIdAndCompanyId(client.getId(), company.getId()).isPresent()) {
      throw new ConflictException("El cliente ya tiene una cuenta de puntos registrada en esta empresa.");
    }

    PointsAccount account = new PointsAccount();
    account.setClient(client);
    account.setCompany(company);
    account.setBalance(0);
    account = pointsAccountRepository.save(account);

    CompanyListDTO companyDTO = new CompanyListDTO(company.getId(), company.getName(), company.getCompanyDetails(), company.getAmountStep(), company.getPointsPerStep(), company.getIsEnabled(), company.getAppAdminOwner());
    ClientDetailDTO clientDTO = new ClientDetailDTO(client.getId(), client.getDni(), client.getCountry(), client.getName(), client.getEmail(), client.getPhone());

    return new PointsAccountDetailDTO(account.getId(), account.getBalance(), companyDTO, clientDTO);
  }


  @Override
  public Page<PointsAccountDetailDTO> listPointsAccounts(
      String companyAdminId, Long companyId, Pageable pageable) {
    companyAccessValidator.validateAccess(companyId, companyAdminId);
    Page<PointsAccount> pointsAccounts =
        pointsAccountRepository.findByCompanyId(companyId, pageable);
    if (pointsAccounts.isEmpty()) {
      return Page.empty();
    }
    return pointsAccounts.map(mapper::toDetailDTO);
  }

  @Override
  @Transactional
  public Page<PointsTransactionDetailDTO> getTransactionHistory(Long clientId, Long companyId, Pageable pageable) {
    PointsAccount pointsAccount = pointsAccountRepository.findByClientIdAndCompanyId(clientId, companyId).orElseThrow(()-> new NotFoundException("PointsAccount not found"));
    Page<PointsTransaction> pointsTransactionList = transactionRepository.findByPointsAccount(pointsAccount, pageable);
    if(pointsTransactionList.isEmpty()) {
      return Page.empty();
    }
    return pointsTransactionList.map(transactionMapper::toDetailDTO);
  }

  //EVENTS
  @EventListener
  @Transactional
  public void handleSaleCreated(SaleCreatedEvent event) {
    log.info("Procesando evento de venta para calcular puntos. Usuario: {}, Empresa: {}",
            event.client().getId(), event.company().getId());

    PointsAccount pointsAccount =
            pointsAccountRepository
                    .findByClientIdAndCompanyId(event.client().getId(), event.company().getId())
                    .orElseThrow(() -> new NotFoundException("Points Account not found"));

    BigDecimal amount = event.amount();
    BigDecimal step = event.company().getAmountStep();
    Integer pointsPerStep = event.company().getPointsPerStep();

    int basePoints = amount.divideToIntegralValue(step).intValue() * pointsPerStep;

    OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);

    BigDecimal multiplier = promotionRepository
            .findActivePromotion(event.company().getId(), nowUtc)
            .map(Promotion::getMultiplier)
            .orElse(BigDecimal.ONE);

    int pointsToEarn = BigDecimal.valueOf(basePoints)
            .multiply(multiplier)
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();

    pointsAccount.setBalance(pointsAccount.getBalance() + pointsToEarn);
    pointsAccount = pointsAccountRepository.save(pointsAccount);

    PointsTransaction transaction = new PointsTransaction();
    transaction.setPointsAccount(pointsAccount);
    transaction.setAmount(pointsToEarn);
    transaction.setTransactionType(TransactionType.EARNED);
    transaction.setCreatedAt(nowUtc);

    transactionRepository.save(transaction);

    log.info("Venta procesada con éxito. Se acreditaron {} puntos (Base: {}, Multiplicador: {}). Nuevo balance: {}",
            pointsToEarn, basePoints, multiplier, pointsAccount.getBalance());
  }

  @EventListener
  @Transactional
  public void deductPoints(RewardRedeemEvent event) {
    PointsAccount account =
        pointsAccountRepository
            .findByClientIdAndCompanyId(event.client().getId(), event.company().getId())
            .orElseThrow(() -> new NotFoundException("Points account not found"));

    if (account.getBalance() < event.costInPoints()) {
      throw new BadRequestException("Insufficient balance in the points account");
    }

    account.setBalance(account.getBalance() - event.costInPoints());

    account = pointsAccountRepository.save(account);

    OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);

    PointsTransaction transaction = new PointsTransaction();
    transaction.setPointsAccount(account);
    transaction.setAmount((event.costInPoints())*(-1));
    transaction.setTransactionType(TransactionType.REDEEMED);
    transaction.setCreatedAt(nowUtc);

    transactionRepository.save(transaction);
  }
}

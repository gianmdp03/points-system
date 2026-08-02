package com.tech.point_system.service.impl;

import com.tech.point_system._enum.TransactionType;
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
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.model.PointsTransaction;
import com.tech.point_system.model.Promotion;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.PointsAccountRepository;
import com.tech.point_system.repository.PointsTransactionRepository;
import com.tech.point_system.repository.PromotionRepository;
import com.tech.point_system._enum.Role;
import com.tech.point_system.dto.user.UserDetailDTO;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.UserRepository;
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
  private final UserRepository userRepository;
  private final PromotionRepository promotionRepository;
  private final CompanyRepository companyRepository;
  private final PointsAccountRepository pointsAccountRepository;
  private final PointsTransactionRepository transactionRepository;
  private final SupabaseAdminClient supabaseAdminClient;
  private final CompanyAccessValidator companyAccessValidator;
  private final PointsTransactionMapper transactionMapper;

  @Override
  @Transactional
  public PointsAccountDetailDTO registerClientAndCreateAccount(
      String companyAdminId, PointsAccountRequestDTO dto) {
    Company company = companyAccessValidator.validateAccess(dto.companyId(), companyAdminId);
    User user = userRepository.findByDni(dto.dni()).orElse(null);
    if (user == null) {
      log.info("Usuario nuevo detectado. Iniciando registro en Supabase para DNI: {}", dto.dni());
      String supabaseUserId = supabaseAdminClient.inviteUser(dto.email(), dto.name(), dto.dni());

      user =
          User.builder()
              .id(supabaseUserId)
              .email(dto.email())
              .name(dto.name())
              .dni(dto.dni())
              .role(Role.USER)
              .isActive(true)
              .build();
      user = userRepository.save(user);
    } else {
      log.info(
          "El usuario ya existe en el sistema (DNI: {}). Procediendo a vincular nueva empresa.",
          dto.dni());
    }

    if (pointsAccountRepository
        .findByUserIdAndCompanyId(user.getId(), company.getId())
        .isPresent()) {
      log.warn(
          "Intento de duplicar cuenta de puntos para el usuario {} en la empresa {}",
          user.getId(),
          company.getId());
      throw new ConflictException(
          "El usuario ya tiene una cuenta de puntos registrada en esta empresa.");
    }

    PointsAccount account = new PointsAccount();
    account.setUser(user);
    account.setCompany(company);
    account.setBalance(0);
    account = pointsAccountRepository.save(account);

    log.info(
        "Cuenta de puntos creada exitosamente para el usuario {} en la empresa {}",
        user.getId(),
        company.getId());

    CompanyListDTO companyDTO =
        new CompanyListDTO(company.getId(), company.getName(), company.getCompanyDetails(), company.getAmountStep(), company.getPointsPerStep(), company.getIsEnabled());

    UserDetailDTO userDTO =
        new UserDetailDTO(
            user.getId(), user.getEmail(), user.getName(), user.getDni(), user.getRole());

    return new PointsAccountDetailDTO(account.getId(), account.getBalance(), companyDTO, userDTO);
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
  public Page<PointsTransactionDetailDTO> getTransactionHistory(String userId, Long companyId, Pageable pageable) {
    PointsAccount pointsAccount = pointsAccountRepository.findByUserIdAndCompanyId(userId, companyId).orElseThrow(()-> new NotFoundException("PointsAccount not found"));
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
            event.user().getId(), event.company().getId());

    PointsAccount pointsAccount =
            pointsAccountRepository
                    .findByUserIdAndCompanyId(event.user().getId(), event.company().getId())
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
            .findByUserIdAndCompanyId(event.user().getId(), event.company().getId())
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

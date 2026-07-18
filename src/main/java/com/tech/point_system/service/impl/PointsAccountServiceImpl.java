package com.tech.point_system.service.impl;

import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import com.tech.point_system.exception.BadRequestException;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.PointsAccountMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.model.PointsTransaction;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.PointsAccountRepository;
import com.tech.point_system.repository.PointsTransactionRepository;
import com.tech.point_system.security.user._enum.Role;
import com.tech.point_system.security.user.dto.user.UserDetailDTO;
import com.tech.point_system.security.user.model.User;
import com.tech.point_system.security.user.repository.UserRepository;
import com.tech.point_system.security.user.service.CompanyAccessValidator;
import com.tech.point_system.security.user.service.SupabaseAdminClient;
import com.tech.point_system.service.PointsAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PointsAccountServiceImpl implements PointsAccountService {
  private final PointsAccountMapper mapper;
  private final UserRepository userRepository;
  private final CompanyRepository companyRepository;
  private final PointsAccountRepository pointsAccountRepository;
  private final PointsTransactionRepository transactionRepository;
  private final SupabaseAdminClient supabaseAdminClient;
  private final CompanyAccessValidator companyAccessValidator;

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
        new CompanyListDTO(company.getId(), company.getName(), company.getCompanyDetails());

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
  public void deductPoints(Long accountId, Integer pointsToDeduct) {
    PointsAccount account =
        pointsAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new NotFoundException("Points account not found"));

    if (account.getBalance() < pointsToDeduct) {
      throw new BadRequestException("Insufficient balance in the points account");
    }

    account.setBalance(account.getBalance() - pointsToDeduct);

    pointsAccountRepository.save(account);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PointsTransaction> getTransactionHistory(Long accountId, Pageable pageable) {
    if (!pointsAccountRepository.existsById(accountId)) {
      throw new NotFoundException("Points account not found");
    }
    return transactionRepository.findByPointsAccountId(accountId, pageable);
  }
}

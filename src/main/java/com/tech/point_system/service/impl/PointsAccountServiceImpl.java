package com.tech.point_system.service.impl;

import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import com.tech.point_system.exception.ConflictException;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.PointsAccountRepository;
import com.tech.point_system.security.user._enum.Role;
import com.tech.point_system.security.user.dto.user.UserDetailDTO;
import com.tech.point_system.security.user.model.User;
import com.tech.point_system.security.user.repository.UserRepository;
import com.tech.point_system.security.user.service.SupabaseAdminClient;
import com.tech.point_system.service.PointsAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PointsAccountServiceImpl implements PointsAccountService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PointsAccountRepository pointsAccountRepository;
    private final SupabaseAdminClient supabaseAdminClient;

    @Override
    public PointsAccountDetailDTO registerClientAndCreateAccount(PointsAccountRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new NotFoundException("La empresa no existe"));
        User user = userRepository.findByDni(dto.dni()).orElse(null);
        if (user == null) {
            log.info("Usuario nuevo detectado. Iniciando registro en Supabase para DNI: {}", dto.dni());
            String supabaseUserId = supabaseAdminClient.inviteUser(dto.email(), dto.name(), dto.dni());

            user = User.builder()
                    .id(supabaseUserId)
                    .email(dto.email())
                    .name(dto.name())
                    .dni(dto.dni())
                    .role(Role.USER)
                    .isActive(true)
                    .build();
            user = userRepository.save(user);
        } else {
            log.info("El usuario ya existe en el sistema (DNI: {}). Procediendo a vincular nueva empresa.", dto.dni());
        }

        if (pointsAccountRepository.findByUserIdAndCompanyId(user.getId(), company.getId()).isPresent()) {
            log.warn("Intento de duplicar cuenta de puntos para el usuario {} en la empresa {}", user.getId(), company.getId());
            throw new ConflictException("El usuario ya tiene una cuenta de puntos registrada en esta empresa.");
        }

        PointsAccount account = new PointsAccount();
        account.setUser(user);
        account.setCompany(company);
        account.setBalance(0);
        account = pointsAccountRepository.save(account);

        log.info("Cuenta de puntos creada exitosamente para el usuario {} en la empresa {}", user.getId(), company.getId());

        CompanyListDTO companyDTO = new CompanyListDTO(
                company.getId(),
                company.getName(),
                company.getCompanyDetails()
        );

        UserDetailDTO userDTO = new UserDetailDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getDni(),
                user.getRole()
        );

        return new PointsAccountDetailDTO(
                account.getId(),
                account.getBalance(),
                companyDTO,
                userDTO
        );
    }
}
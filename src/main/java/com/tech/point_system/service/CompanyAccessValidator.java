package com.tech.point_system.service;

import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.model.Company;
import com.tech.point_system.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.tech.point_system.model.User;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyAccessValidator {
    private final CompanyRepository companyRepository;

    public Company validateAccess(Long companyId, String userId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAppAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_APP_ADMIN"));

        if (isAppAdmin) {
            log.info("Acceso concedido por rol APP_ADMIN a la empresa {}", companyId);
            return company;
        }

        if (company.getAdmin() == null || !company.getAdmin().getId().equals(userId)) {
            log.warn("Intento de vulnerabilidad IDOR bloqueado. El usuario {} intentó acceder a la empresa {}", userId, companyId);
            throw new AccessDeniedException("Acceso denegado: No tienes permisos para modificar los datos de esta empresa");
        }

        User admin = company.getAdmin();
        if (Boolean.TRUE.equals(admin.getIsSuspendedForChargeback())
                || (admin.getPendingDebtArs() != null && admin.getPendingDebtArs().compareTo(BigDecimal.ZERO) > 0)) {
            log.warn("Acceso denegado: Usuario '{}' suspendido por contracargo con deuda pendiente de {} ARS",
                    userId, admin.getPendingDebtArs());
            throw new AccessDeniedException("Tu cuenta se encuentra suspendida por saldo deudor pendiente ($"
                    + (admin.getPendingDebtArs() != null ? admin.getPendingDebtArs() : BigDecimal.ZERO)
                    + " ARS). Regularizá tu pago para volver a operar.");
        }

        return company;
    }

    public void checkAccessOnly(Long companyId, String userId) {
        validateAccess(companyId, userId);
    }
}
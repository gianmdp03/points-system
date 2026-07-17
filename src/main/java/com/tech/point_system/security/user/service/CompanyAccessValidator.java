package com.tech.point_system.security.user.service;

import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.model.Company;
import com.tech.point_system.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyAccessValidator {

    private final CompanyRepository companyRepository;

    public Company validateAccess(Long companyId, String userId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("La empresa no existe"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAppAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_APP_ADMIN"));

        boolean isUser = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

        if (isAppAdmin || isUser) {
            log.info("Acceso concedido por rol {} a la empresa {}", isAppAdmin ? "APP_ADMIN" : "USER", companyId);
            return company;
        }

        if (company.getAdmin() == null || !company.getAdmin().getId().equals(userId)) {
            log.warn("Intento de vulnerabilidad IDOR bloqueado. El usuario {} intentó acceder a la empresa {}", userId, companyId);
            throw new AccessDeniedException("Acceso denegado: No tienes permisos para modificar los datos de esta empresa");
        }

        return company;
    }
    
    public void checkAccessOnly(Long companyId, String userId) {
        validateAccess(companyId, userId);
    }
}
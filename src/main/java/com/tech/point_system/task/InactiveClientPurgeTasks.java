package com.tech.point_system.task;

import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.PointsAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InactiveClientPurgeTasks {

    private final CompanyRepository companyRepository;
    private final PointsAccountRepository pointsAccountRepository;

    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void purgeInactiveClients() {
        log.info("Iniciando tarea programada: purga de clientes inactivos...");

        List<Company> companiesWithPurge = companyRepository.findByIsInactiveClientPurgeEnabledTrueAndInactiveClientPurgeDaysIsNotNull();

        if (companiesWithPurge.isEmpty()) {
            log.debug("No hay empresas con política de purga de clientes inactivos activada.");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int totalPurged = 0;

        for (Company company : companiesWithPurge) {
            Integer days = company.getInactiveClientPurgeDays();
            if (days == null || days <= 0) {
                continue;
            }

            OffsetDateTime threshold = now.minusDays(days);
            List<PointsAccount> inactiveAccounts = pointsAccountRepository.findByCompanyIdAndLastActivityDateBefore(company.getId(), threshold);

            if (!inactiveAccounts.isEmpty()) {
                log.info("Empresa ID {}: se encontraron {} cuentas inactivas por más de {} días para purgar.",
                        company.getId(), inactiveAccounts.size(), days);
                pointsAccountRepository.deleteAll(inactiveAccounts);
                totalPurged += inactiveAccounts.size();
            }
        }

        log.info("Tarea de purga de clientes inactivos finalizada: se eliminaron {} cuentas en total.", totalPurged);
    }
}

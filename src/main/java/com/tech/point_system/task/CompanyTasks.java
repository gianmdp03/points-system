package com.tech.point_system.task;

import com.tech.point_system.model.Company;
import com.tech.point_system.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyTasks {
    private final CompanyRepository companyRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteDisabledCompanies(){
        log.info("Iniciando tarea programada: limpieza de compañías deshabilitadas hace más de 30 días...");

        OffsetDateTime thresholdDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);
        List<Company> companiesToDelete = companyRepository.findByIsEnabledFalseAndDisabledDateBefore(thresholdDate);

        if (!companiesToDelete.isEmpty()) {
            companyRepository.deleteAll(companiesToDelete);
            log.info("Se eliminaron permanentemente {} compañías de la base de datos.", companiesToDelete.size());
        } else {
            log.debug("No se encontraron compañías para eliminar en este ciclo.");
        }
    }
}

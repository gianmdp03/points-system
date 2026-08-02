package com.tech.point_system.task;

import com.tech.point_system._enum.Role;
import com.tech.point_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTasks {
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void disableCompanyAdminsFreeTrials() {
        log.info("Iniciando tarea programada: comprobación en lote de pruebas gratuitas vencidas para COMPANY_ADMIN...");

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        int updatedCount = userRepository.disableExpiredFreeTrials(today, Role.COMPANY_ADMIN);

        if (updatedCount > 0) {
            log.info("Se finalizó la prueba gratuita masivamente para {} administrador(es) de empresa.", updatedCount);
        } else {
            log.debug("No se encontraron pruebas gratuitas expiradas en el día de hoy.");
        }
    }
}
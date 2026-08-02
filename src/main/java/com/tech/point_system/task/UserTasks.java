package com.tech.point_system.task;

import com.tech.point_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTasks {
    @Value("${app.freetrial.duration-days:30}")
    private int daysOfFreeTrial;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void disableCompanyAdminsFreeTrials() {
        log.info("Iniciando tarea programada: actualización en lote de pruebas gratuitas expiradas...");

        OffsetDateTime thresholdDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(daysOfFreeTrial);

        int updatedCount = userRepository.disableExpiredFreeTrials(thresholdDate);

        if (updatedCount > 0) {
            log.info("Se finalizó la prueba gratuita masivamente para {} usuario(s).", updatedCount);
        } else {
            log.debug("No se encontraron pruebas gratuitas expiradas para actualizar en este ciclo.");
        }
    }
}
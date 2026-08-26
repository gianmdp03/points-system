package com.tech.point_system.task;

import com.tech.point_system._enum.Role;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationTasks {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Tarea programada diaria para expirar planes de usuarios cuya fecha de cobertura prepaga
     * haya caducado (User.planExpirationDate < now), seteando currentPlan = NONE.
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void processSubscriptionExpirations() {
        log.info("[SUBSCRIPTION EXPIRATION TASK] ⏰ [CRON 04:00 AM] Verificando vencimiento de planes prepagos y pruebas gratuitas...");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int expiredPlansCount = userRepository.expireUserPlans(SubscriptionPlan.NONE, now);

        if (expiredPlansCount > 0) {
            log.info("[SUBSCRIPTION EXPIRATION TASK] 🏁 {} planes de usuarios vencidos fueron expirados a NONE masivamente en Batch.", expiredPlansCount);
        } else {
            log.info("[SUBSCRIPTION EXPIRATION TASK] ℹ️ No hay planes de usuarios vencidos en el ciclo actual.");
        }

        // Expirar periodos de prueba gratuita vencidos
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int expiredTrials = userRepository.disableExpiredFreeTrials(today, Role.COMPANY_ADMIN);
        if (expiredTrials > 0) {
            log.info("[SUBSCRIPTION EXPIRATION TASK] 🏁 {} pruebas gratuitas vencidas desactivadas.", expiredTrials);
        }
    }

    /**
     * Tarea programada horaria para purgar órdenes de compra PENDING abandonadas
     * con más de 24 horas de antigüedad, evitando que la base de datos acumule registros innecesarios.
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void purgeAbandonedPendingSubscriptions() {
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC).minusHours(24);
        int deleted = subscriptionRepository.deleteByStatusAndCreatedAtBefore(SubscriptionStatus.PENDING, threshold);
        if (deleted > 0) {
            log.info("[SUBSCRIPTION CLEANUP TASK] 🧹 Se eliminaron {} órdenes de suscripción PENDING abandonadas (> 24hs).", deleted);
        }
    }
}


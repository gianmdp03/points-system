package com.tech.point_system.task;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.model.PointsTransaction;
import com.tech.point_system.repository.PointsTransactionRepository;
import com.tech.point_system.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpirationNotificationTasks {

    private final PointsTransactionRepository transactionRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(readOnly = true)
    public void sendPointsExpirationAlerts() {
        log.info("[EXPIRATION ALERTS TASK] 🚀 Iniciando tarea programada: Notificaciones preventivas de vencimiento de puntos...");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Hito 1: Puntos que vencen en 30 días (1 mes)
        processMilestone(now.plusDays(29), now.plusDays(30), 30);

        // Hito 2: Puntos que vencen en 10 días
        processMilestone(now.plusDays(9), now.plusDays(10), 10);

        log.info("[EXPIRATION ALERTS TASK] ✅ Tarea de notificaciones de vencimiento finalizada.");
    }

    private void processMilestone(OffsetDateTime startWindow, OffsetDateTime endWindow, int daysRemaining) {
        List<PointsTransaction> transactions = transactionRepository
                .findTransactionsExpiringBetween(startWindow, endWindow);

        if (transactions.isEmpty()) {
            log.debug("[EXPIRATION ALERTS TASK] No se encontraron transacciones que venzan en el hito de {} días.", daysRemaining);
            return;
        }

        // Agrupación O(N) por cuenta para evitar envíos múltiples si el cliente tiene varias transacciones que vencen el mismo día
        Map<PointsAccount, Integer> expiringByAccount = new HashMap<>();
        for (PointsTransaction tx : transactions) {
            PointsAccount account = tx.getPointsAccount();
            if (account != null) {
                int amount = tx.getAvailableAmount() != null ? tx.getAvailableAmount() : 0;
                expiringByAccount.merge(account, amount, Integer::sum);
            }
        }

        int sentCount = 0;
        for (Map.Entry<PointsAccount, Integer> entry : expiringByAccount.entrySet()) {
            PointsAccount account = entry.getKey();
            int expiringPoints = entry.getValue();

            if (expiringPoints <= 0) {
                continue;
            }

            Company company = account.getCompany();
            Client client = account.getClient();

            if (company == null || client == null) {
                continue;
            }

            Map<String, Object> extraParams = new HashMap<>();
            extraParams.put("pointsExpiring", expiringPoints);
            extraParams.put("pointsBalance", account.getBalance() != null ? account.getBalance() : expiringPoints);
            extraParams.put("currentPoints", expiringPoints);
            extraParams.put("puntos", expiringPoints);
            extraParams.put("points", expiringPoints);
            extraParams.put("expirationDays", daysRemaining);
            extraParams.put("dias", daysRemaining);
            extraParams.put("localName", company.getName());
            extraParams.put("local", company.getName());
            extraParams.put("empresa", company.getName());

            emailService.sendNotificationEmail(
                    NotificationType.POINTS_EXPIRATION_NOTIFICATION,
                    company,
                    client,
                    extraParams
            );
            sentCount++;
        }

        log.info("[EXPIRATION ALERTS TASK] Hito {} días: Se enviaron {} avisos de vencimiento preventivo.",
                daysRemaining, sentCount);
    }
}

package com.tech.point_system.task;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.PointsAccountRepository;
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
public class CustomerRetentionTasks {

    private final CompanyRepository companyRepository;
    private final PointsAccountRepository pointsAccountRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 30 9 * * ?")
    @Transactional
    public void sendCustomerRetentionNotifications() {
        log.info("[RETENTION TASK] 🚀 Iniciando tarea programada: Retención automática de clientes inactivos...");

        List<Company> companies = companyRepository.findByIsClientRetentionEnabledTrueAndClientRetentionDaysIsNotNull();
        if (companies.isEmpty()) {
            log.debug("[RETENTION TASK] No hay comercios con política de retención automática de clientes activada.");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int totalNotificationsSent = 0;

        for (Company company : companies) {
            Integer intervalDays = company.getClientRetentionDays();
            if (intervalDays == null || intervalDays <= 0) {
                continue;
            }

            OffsetDateTime thresholdDate = now.minusDays(intervalDays);
            List<PointsAccount> eligibleAccounts = pointsAccountRepository
                    .findEligibleForRetentionNotification(company.getId(), thresholdDate);

            if (eligibleAccounts.isEmpty()) {
                continue;
            }

            log.info("[RETENTION TASK] Comercio '{}' (ID: {}): {} clientes cumplen el umbral de {} días de inactividad para recordatorio.",
                    company.getName(), company.getId(), eligibleAccounts.size(), intervalDays);

            for (PointsAccount account : eligibleAccounts) {
                Client client = account.getClient();
                if (client == null) {
                    continue;
                }

                Map<String, Object> extraParams = new HashMap<>();
                int balance = account.getBalance() != null ? account.getBalance() : 0;
                extraParams.put("pointsBalance", balance);
                extraParams.put("currentPoints", balance);
                extraParams.put("puntos", balance);
                extraParams.put("points", balance);
                extraParams.put("localName", company.getName());
                extraParams.put("local", company.getName());
                extraParams.put("empresa", company.getName());

                emailService.sendNotificationEmail(
                        NotificationType.CLIENT_RETENTION_NOTIFICATION,
                        company,
                        client,
                        extraParams
                );

                account.setLastRetentionNotificationDate(now);
                totalNotificationsSent++;
            }

            pointsAccountRepository.saveAll(eligibleAccounts);
        }

        log.info("[RETENTION TASK] ✅ Tarea de retención finalizada: se despacharon {} notificaciones de reactivación en total.",
                totalNotificationsSent);
    }
}

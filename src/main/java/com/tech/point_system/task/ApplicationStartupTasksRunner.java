package com.tech.point_system.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStartupTasksRunner {

    private final UserTasks userTasks;
    private final CompanyTasks companyTasks;
    private final PointsExpirationTasks pointsExpirationTasks;
    private final InactiveClientPurgeTasks inactiveClientPurgeTasks;
    private final SubscriptionExpirationTasks subscriptionExpirationTasks;
    private final com.tech.point_system.service.MessageTemplateService messageTemplateService;

    /**
     * Se ejecuta automáticamente y de forma asíncrona una vez que la aplicación arranca (ApplicationReadyEvent).
     * Si el servidor estuvo apagado durante las horas programadas de los cron jobs, este runner garantiza
     * la consistencia y seguridad del sistema ejecutando de inmediato las tareas pendientes.
     */
    @Async("taskExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void runAllTasksOnStartup() {
        log.info("==================================================================");
        log.info("[STARTUP RECOVERY] Ejecutando verificación de tareas de mantenimiento iniciales...");
        log.info("==================================================================");

        try {
            log.info("[STARTUP] 1/5 Verificando pruebas gratuitas vencidas...");
            userTasks.disableCompanyAdminsFreeTrials();
        } catch (Exception e) {
            log.error("[STARTUP ERROR] Falló la verificación de pruebas gratuitas al iniciar:", e);
        }

        try {
            log.info("[STARTUP] 2/5 Verificando compañías deshabilitadas para eliminación...");
            companyTasks.deleteDisabledCompanies();
        } catch (Exception e) {
            log.error("[STARTUP ERROR] Falló la eliminación de compañías deshabilitadas al iniciar:", e);
        }

        try {
            log.info("[STARTUP] 3/5 Verificando vencimiento de puntos FIFO...");
            pointsExpirationTasks.expirePoints();
        } catch (Exception e) {
            log.error("[STARTUP ERROR] Falló el vencimiento de puntos al iniciar:", e);
        }

        try {
            log.info("[STARTUP] 4/5 Verificando purga de clientes inactivos...");
            inactiveClientPurgeTasks.purgeInactiveClients();
        } catch (Exception e) {
            log.error("[STARTUP ERROR] Falló la purga de clientes inactivos al iniciar:", e);
        }

        try {
            log.info("[STARTUP] 5/5 Verificando suscripciones prepagas expiradas y limpiando órdenes PENDING abandonadas...");
            subscriptionExpirationTasks.processSubscriptionExpirations();
            subscriptionExpirationTasks.purgeAbandonedPendingSubscriptions();
        } catch (Exception e) {
            log.error("[STARTUP ERROR] Falló la verificación de suscripciones expiradas al iniciar:", e);
        }

        try {
            log.info("[STARTUP] 6/6 Verificando siembra de plantillas de mensajes por defecto...");
            messageTemplateService.seedDefaultTemplatesForAllCompaniesWithoutTemplates();
        } catch (Exception e) {
            log.error("[STARTUP ERROR] Falló la siembra de plantillas de mensajes al iniciar:", e);
        }

        log.info("==================================================================");
        log.info("[STARTUP RECOVERY] Todas las tareas de mantenimiento completadas exitosamente.");
        log.info("==================================================================");
    }
}


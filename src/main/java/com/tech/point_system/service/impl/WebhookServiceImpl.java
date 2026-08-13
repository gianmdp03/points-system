package com.tech.point_system.service.impl;

import com.tech.point_system._enum.Role;
import com.tech.point_system.dto.supabaseWebhook.SupabaseWebhookDTO;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.repository.UserRepository;
import com.tech.point_system.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public void processUserWebhook(SupabaseWebhookDTO payload) {
        if (!"users".equals(payload.table())) {
            log.debug("Evento de webhook ignorado para tabla: {}", payload.table());
            return;
        }

        if ("INSERT".equals(payload.type())) {
            handleInsert(payload);
        } else if ("DELETE".equals(payload.type())) {
            handleDelete(payload);
        } else {
            log.debug("Tipo de evento no soportado: {}", payload.type());
        }
    }

    private void handleInsert(SupabaseWebhookDTO payload) {
        if (payload.record() == null) {
            log.warn("El webhook INSERT no contiene el objeto 'record'. Abortando.");
            return;
        }

        String userId = payload.record().id();
        String email = payload.record().email();

        if (!StringUtils.hasText(userId) || !StringUtils.hasText(email)) {
            log.warn("Datos críticos faltantes en el webhook (ID o Email nulos/vacíos). Payload ID: {}", userId);
            return;
        }

        if (userRepository.existsById(userId)) {
            log.info("El usuario con UUID {} ya existe. Ignorando webhook duplicado.", userId);
            return;
        }

        try {
            Map<String, Object> metadata = payload.record().rawUserMetaData();
            String name = extractFromMap(metadata, "name", "Usuario sin nombre");
            String dni = extractFromMap(metadata, "dni", "No registrado");

            String requestedRoleStr = extractFromMap(metadata, "role", "COMPANY_ADMIN");
            Role assignedRole;

            try {
                Role roleEnum = Role.valueOf(requestedRoleStr.toUpperCase());
                if (roleEnum == Role.APP_ADMIN) {
                    log.warn("Intento de registro no permitido como APP_ADMIN para el email: {}. Asignando COMPANY_ADMIN.", email);
                    assignedRole = Role.COMPANY_ADMIN;
                } else {
                    assignedRole = roleEnum;
                }
            } catch (IllegalArgumentException e) {
                log.warn("Rol inválido ({}) para email: {}. Asignando COMPANY_ADMIN por defecto.", requestedRoleStr, email);
                assignedRole = Role.COMPANY_ADMIN;
            }

            User newUser = User.builder()
                    .id(userId)
                    .email(email)
                    .name(name)
                    .dni(dni)
                    .role(assignedRole)
                    .build();

            userRepository.save(newUser);
            log.info("Nuevo usuario sincronizado con éxito. UUID: {} | Email: {}", newUser.getId(), newUser.getEmail());

        } catch (Exception e) {
            log.error("Fallo crítico al guardar el usuario desde el webhook. Payload ID: {}", userId, e);
        }
    }

    private void handleDelete(SupabaseWebhookDTO payload) {
        if (payload.oldRecord() == null || payload.oldRecord().id() == null) {
            log.warn("El webhook DELETE no contiene el objeto 'old_record' o un ID válido. Abortando.");
            return;
        }

        String userId = payload.oldRecord().id();
        log.info("Iniciando eliminación en cascada para el usuario UUID: {}", userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.info("El usuario UUID {} no existe en la base de datos local. Nada que borrar.", userId);
            return;
        }

        try {
            subscriptionRepository.findByUserId(userId).ifPresent(subscription -> {
                subscriptionRepository.delete(subscription);
                log.info("Suscripción eliminada para el usuario UUID: {}", userId);
            });

            List<Company> userCompanies = companyRepository.findAllByAdminId(userId);
            if (!userCompanies.isEmpty()) {
                companyRepository.deleteAll(userCompanies);
                log.info("Se eliminaron {} empresas vinculadas al usuario UUID: {}", userCompanies.size(), userId);
            }

            userRepository.delete(user);
            log.info("Usuario UUID {} eliminado con éxito.", userId);

        } catch (Exception e) {
            log.error("Fallo crítico al intentar eliminar los datos en cascada del usuario UUID: {}", userId, e);
        }
    }

    private String extractFromMap(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            String value = map.get(key).toString().trim();
            return StringUtils.hasText(value) ? value : defaultValue;
        }
        return defaultValue;
    }
}
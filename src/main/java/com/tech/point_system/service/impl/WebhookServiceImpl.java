package com.tech.point_system.service.impl;

import com.tech.point_system._enum.Role;
import com.tech.point_system.dto.supabaseWebhook.SupabaseWebhookDTO;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.UserRepository;
import com.tech.point_system.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final UserRepository userRepository;

    @Transactional
    public void processUserWebhook(SupabaseWebhookDTO payload) {
        if (!"INSERT".equals(payload.type()) || !"users".equals(payload.table())) {
            log.debug("Evento de webhook ignorado. Tipo: {}, Tabla: {}", payload.type(), payload.table());
            return;
        }

        if (payload.record() == null) {
            log.warn("El webhook recibido no contiene el objeto 'record'. Abortando.");
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

            String requestedRoleStr = extractFromMap(metadata, "role", "USER");

            Role assignedRole;
            try {
                Role roleEnum = Role.valueOf(requestedRoleStr.toUpperCase());
                if (roleEnum == Role.APP_ADMIN) {
                    log.warn("Intento de registro no permitido como APP_ADMIN para el email: {}. Asignando USER.", email);
                    assignedRole = Role.USER;
                } else {
                    assignedRole = roleEnum;
                }
            } catch (IllegalArgumentException e) {
                log.warn("Rol inválido ({}) para email: {}. Asignando USER por defecto.", requestedRoleStr, email);
                assignedRole = Role.USER;
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

    private String extractFromMap(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            String value = map.get(key).toString().trim();
            return StringUtils.hasText(value) ? value : defaultValue;
        }
        return defaultValue;
    }
}
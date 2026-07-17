package com.tech.point_system.security.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Service
@Slf4j
public class SupabaseAdminClient {
    private final RestClient restClient;

    public SupabaseAdminClient(
            @Value("${supabase.project.url}") String supabaseUrl,
            @Value("${supabase.service.role.key}") String serviceRoleKey) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl + "/auth/v1")
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String inviteUser(String email, String name, String dni) {
        log.info("Iniciando invitacion via Supabase para el email: {}", email);

        try {
            Map<String, Object> payload = Map.of(
                    "email", email,
                    "data", Map.of(
                            "name", name,
                            "dni", dni
                    )
            );

            var response = restClient.post()
                    .uri("/invite")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String userId = (String) response.get("id");
            log.info("Usuario invitado exitosamente en Supabase. UUID asignado: {}", userId);

            return userId;

        } catch (Exception e) {
            log.error("Fallo critico al invitar usuario en Supabase API: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo procesar la invitacion en Supabase", e);
        }
    }
}
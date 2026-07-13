package com.tech.point_system.security.user.controller;

import com.tech.point_system.security.user.dto.supabaseWebhook.SupabaseWebhookDTO;
import com.tech.point_system.security.user.model.User;
import com.tech.point_system.security.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

  private final UserRepository userRepository;

  @Value("${spring.datasource.password}")
  private String webhookSecret;

  @PostMapping("/supabase")
  public ResponseEntity<String> handleSupabaseWebhook(
      @RequestHeader("X-Supabase-Secret") String secret, @RequestBody SupabaseWebhookDTO payload) {

    if (!webhookSecret.equals(secret)) {
      log.warn("Intento de acceso no autorizado al webhook de Supabase");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Secret inválido");
    }

    if ("INSERT".equals(payload.type()) && "users".equals(payload.table())) {
      try {
        User newUser = new User();
        newUser.setId(payload.record().id());
        newUser.setEmail(payload.record().email());
        Map<String, Object> metadata = payload.record().rawUserMetaData();
        if (metadata != null) {
          if (metadata.containsKey("name")) {
            newUser.setName(metadata.get("name").toString());
          }
          if (metadata.containsKey("dni")) {
            newUser.setDni(metadata.get("dni").toString());
          }
        }
        userRepository.save(newUser);
        log.info("Nuevo usuario sincronizado con éxito. UUID: {}", newUser.getId());

      } catch (Exception e) {
        log.error("Error guardando el usuario desde el webhook", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      }
    }

    return ResponseEntity.ok("Webhook procesado");
  }
}

package com.tech.point_system.controller;

import com.tech.point_system.dto.supabaseWebhook.SupabaseWebhookDTO;
// Asegúrate de importar el servicio o su interfaz correspondiente
import com.tech.point_system.service.impl.WebhookServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

  private final WebhookServiceImpl webhookService;

  @Value("${supabase.webhook.secret}")
  private String webhookSecret;

  @PostMapping("/supabase")
  public ResponseEntity<String> handleSupabaseWebhook(
          @RequestHeader(value = "X-Supabase-Secret", required = false) String secret,
          @RequestBody SupabaseWebhookDTO payload) {

    if (secret == null || !webhookSecret.equals(secret)) {
      log.warn("Intento de acceso denegado al webhook: Secret inválido o ausente");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Secret inválido");
    }

    webhookService.processUserWebhook(payload);

    return ResponseEntity.ok("Webhook procesado");
  }
}
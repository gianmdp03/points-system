package com.tech.point_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.payment.PaymentStrategyFactory;
import com.tech.point_system.payment.mercadopago.MercadoPagoSignatureValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class SubscriptionWebhookController {

    private final PaymentStrategyFactory paymentStrategyFactory;
    private final MercadoPagoSignatureValidator mercadoPagoSignatureValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping(
            value = "/subscriptions/{provider}",
            method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.HEAD}
    )
    public ResponseEntity<Void> handleWebhook(
            @PathVariable("provider") String providerStr,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "user-agent", required = false) String userAgent,
            @RequestParam(required = false) Map<String, String> queryParams,
            HttpServletRequest request) {

        String httpMethod = request.getMethod();
        log.info("[WEBHOOK CONTROLLER] 📥 [HTTP {}] Notificación Webhook recibida | ProviderRaw='{}' | URI='{}' | Query='{}'",
                httpMethod, providerStr, request.getRequestURI(), request.getQueryString());
        log.info("[WEBHOOK CONTROLLER] 🛡️ Headers | x-request-id='{}' | x-signature='{}' | User-Agent='{}' | Content-Type='{}'",
                xRequestId, xSignature, userAgent, request.getContentType());

        // Responder 200 OK inmediatamente a pings / validaciones HEAD o GET sin parámetros
        if ("HEAD".equalsIgnoreCase(httpMethod) || ("GET".equalsIgnoreCase(httpMethod) && (queryParams == null || queryParams.isEmpty()))) {
            log.info("[WEBHOOK CONTROLLER] 🏓 Ping / Healthcheck HEAD o GET recibido de pasarela de pago. Respondiendo 200 OK.");
            return ResponseEntity.ok().build();
        }

        // Resolución tolerante a mayúsculas/minúsculas y formatos de provider
        PaymentProvider provider = resolveProvider(providerStr);
        if (provider == null) {
            log.warn("[WEBHOOK CONTROLLER] ⚠️ Proveedor de pago desconocido: '{}'. Retornando 400 Bad Request.", providerStr);
            return ResponseEntity.badRequest().build();
        }

        // Combinar datos de query params, form-urlencoded y JSON body de forma segura sin fallos 415
        Map<String, Object> combinedPayload = extractPayload(request, queryParams);
        log.info("[WEBHOOK CONTROLLER] 📦 Payload combinado procesable ({} campos): {}", combinedPayload.size(), combinedPayload);

        // Validación de firma de seguridad para Mercado Pago
        if (provider == PaymentProvider.MERCADO_PAGO) {
            String dataId = extractDataId(queryParams, combinedPayload);
            log.info("[WEBHOOK CONTROLLER] 🔐 Verificando firma HMAC-SHA256 para Mercado Pago (DataId='{}', RequestId='{}')",
                    dataId, xRequestId);

            boolean isValid = mercadoPagoSignatureValidator.isValidSignature(xSignature, xRequestId, dataId);
            if (!isValid) {
                log.warn("[WEBHOOK CONTROLLER] ⛔ [RECHAZADO 401 UNAUTHORIZED] Firma de webhook inválida o ausente para xRequestId='{}', dataId='{}'",
                        xRequestId, dataId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            log.info("[WEBHOOK CONTROLLER] ✅ Firma de webhook verificada o autorizada en entorno sandbox.");
        }

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(provider);
        log.info("[WEBHOOK CONTROLLER] 🚀 Despachando procesamiento asíncrono del webhook a la estrategia {}", strategy.getClass().getSimpleName());

        CompletableFuture.runAsync(() -> {
            try {
                strategy.processWebhook(combinedPayload);
                log.info("[WEBHOOK CONTROLLER] ✅ Procesamiento asíncrono de webhook completado exitosamente para {}", provider);
            } catch (Exception e) {
                log.error("[WEBHOOK CONTROLLER] ❌ Error en ejecución asíncrona de webhook para {}", provider, e);
            }
        });

        return ResponseEntity.ok().build();
    }

    private PaymentProvider resolveProvider(String providerStr) {
        if (!StringUtils.hasText(providerStr)) {
            return null;
        }
        String normalized = providerStr.trim().toUpperCase().replace("-", "_");
        if (normalized.equals("MERCADOPAGO") || normalized.equals("MP")) {
            return PaymentProvider.MERCADO_PAGO;
        }
        try {
            return PaymentProvider.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(HttpServletRequest request, Map<String, String> queryParams) {
        Map<String, Object> combinedPayload = new HashMap<>();

        // 1. Query Params
        if (queryParams != null) {
            combinedPayload.putAll(queryParams);
        }

        // 2. Form URL Encoded Parameters (request.getParameterMap)
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap != null) {
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                if (entry.getValue() != null && entry.getValue().length > 0) {
                    combinedPayload.put(entry.getKey(), entry.getValue()[0]);
                }
            }
        }

        // 3. JSON Body (si el content-type es application/json)
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            try (InputStream is = request.getInputStream()) {
                byte[] bodyBytes = is.readAllBytes();
                if (bodyBytes.length > 0) {
                    Map<String, Object> bodyMap = objectMapper.readValue(bodyBytes, Map.class);
                    if (bodyMap != null) {
                        combinedPayload.putAll(bodyMap);
                    }
                }
            } catch (Exception e) {
                log.warn("[WEBHOOK CONTROLLER] ⚠️ No se pudo deserializar el cuerpo JSON del webhook: {}", e.getMessage());
            }
        }

        return combinedPayload;
    }

    @SuppressWarnings("unchecked")
    private String extractDataId(Map<String, String> queryParams, Map<String, Object> bodyPayload) {
        if (queryParams != null) {
            if (queryParams.containsKey("data.id")) return queryParams.get("data.id");
            if (queryParams.containsKey("id")) return queryParams.get("id");
            if (queryParams.containsKey("data_id")) return queryParams.get("data_id");
        }
        if (bodyPayload != null) {
            Object dataObj = bodyPayload.get("data");
            if (dataObj instanceof Map<?, ?> dataMap) {
                Object idObj = dataMap.get("id");
                if (idObj != null) return idObj.toString();
            }
            Object directId = bodyPayload.get("id");
            if (directId != null) return directId.toString();
            Object dotId = bodyPayload.get("data.id");
            if (dotId != null) return dotId.toString();
            Object resource = bodyPayload.get("resource");
            if (resource != null) {
                String resStr = resource.toString();
                int lastSlash = resStr.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < resStr.length() - 1) {
                    return resStr.substring(lastSlash + 1);
                }
            }
        }
        return "";
    }
}


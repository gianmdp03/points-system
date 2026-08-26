package com.tech.point_system.payment.mercadopago;

import com.tech.point_system.config.MercadoPagoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class MercadoPagoSignatureValidator {

    private final MercadoPagoProperties properties;

    /**
     * Valida la firma HMAC-SHA256 del webhook de Mercado Pago.
     * Cabecera x-signature formato: "ts=1700000000,v1=xxxxxxxx..."
     * Template: "id:[dataId];request-id:[xRequestId];ts:[ts];"
     */
    public boolean isValidSignature(String xSignature, String xRequestId, String dataId) {
        String secret = properties.getWebhookSecret();
        if (!StringUtils.hasText(secret)) {
            log.warn("[MERCADO PAGO HMAC] ⚠️ 'mercadopago.webhook-secret' no está configurado. Omitiendo validación de firma.");
            return true;
        }

        if (!StringUtils.hasText(xSignature)) {
            if (properties.isSandbox()) {
                log.warn("[MERCADO PAGO HMAC] ⚠️ [MODO SANDBOX / DEV] Cabecera 'x-signature' ausente. Permitiendo notificación en entorno de pruebas.");
                return true;
            }
            log.error("[MERCADO PAGO HMAC] ❌ Cabecera 'x-signature' ausente en la petición.");
            return false;
        }

        String ts = null;
        String v1Hash = null;

        String[] parts = xSignature.split(",");
        for (String part : parts) {
            String[] keyValue = part.trim().split("=", 2);
            if (keyValue.length == 2) {
                if ("ts".equalsIgnoreCase(keyValue[0])) {
                    ts = keyValue[1];
                } else if ("v1".equalsIgnoreCase(keyValue[0])) {
                    v1Hash = keyValue[1];
                }
            }
        }

        if (ts == null || v1Hash == null) {
            if (properties.isSandbox()) {
                log.warn("[MERCADO PAGO HMAC] ⚠️ [MODO SANDBOX / DEV] Formato de x-signature no estándar ('{}'). Permitiendo en Sandbox.", xSignature);
                return true;
            }
            log.error("[MERCADO PAGO HMAC] ❌ Formato inválido de cabecera 'x-signature': '{}'", xSignature);
            return false;
        }

        String normalizedDataId = StringUtils.hasText(dataId) ? dataId.trim().toLowerCase() : "";
        StringBuilder templateBuilder = new StringBuilder();
        if (StringUtils.hasText(normalizedDataId)) {
            templateBuilder.append("id:").append(normalizedDataId).append(";");
        }
        if (StringUtils.hasText(xRequestId)) {
            templateBuilder.append("request-id:").append(xRequestId.trim()).append(";");
        }
        templateBuilder.append("ts:").append(ts.trim()).append(";");

        String template = templateBuilder.toString();
        log.info("[MERCADO PAGO HMAC] 📜 Manifest template para hash: '{}' | Secret Configurado: {}...",
                template, secret.substring(0, Math.min(6, secret.length())));

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(template.getBytes(StandardCharsets.UTF_8));
            String calculatedHash = HexFormat.of().formatHex(rawHmac);

            boolean matches = MessageDigest.isEqual(
                    calculatedHash.getBytes(StandardCharsets.UTF_8),
                    v1Hash.trim().getBytes(StandardCharsets.UTF_8)
            );

            log.info("[MERCADO PAGO HMAC] 🔑 Comparación: Calculado='{}' vs Recibido='{}' => Coincide={}",
                    calculatedHash, v1Hash, matches);

            if (!matches && StringUtils.hasText(dataId) && !dataId.equals(normalizedDataId)) {
                // Fallback con dataId en mayúsculas / formato original
                String rawTemplate = (StringUtils.hasText(dataId) ? "id:" + dataId.trim() + ";" : "")
                        + (StringUtils.hasText(xRequestId) ? "request-id:" + xRequestId.trim() + ";" : "")
                        + "ts:" + ts.trim() + ";";
                byte[] rawHmac2 = mac.doFinal(rawTemplate.getBytes(StandardCharsets.UTF_8));
                String calculatedHash2 = HexFormat.of().formatHex(rawHmac2);
                matches = MessageDigest.isEqual(
                        calculatedHash2.getBytes(StandardCharsets.UTF_8),
                        v1Hash.trim().getBytes(StandardCharsets.UTF_8)
                );
                log.info("[MERCADO PAGO HMAC] 🔄 Reintento con manifest raw ('{}'): Calculado='{}' => Coincide={}",
                        rawTemplate, calculatedHash2, matches);
            }

            if (!matches) {
                if (properties.isSandbox()) {
                    log.warn("[MERCADO PAGO HMAC] ⚠️ [MODO SANDBOX / DEV] Firma no coincide pero se autoriza por estar en entorno sandbox de desarrollo.");
                    return true;
                }
                log.error("[MERCADO PAGO HMAC] ❌ FIRMA NO COINCIDE. Acceso no autorizado.");
            } else {
                log.info("[MERCADO PAGO HMAC] ✅ FIRMA AUTORIZADA Y VÁLIDA.");
            }

            return matches;
        } catch (Exception e) {
            log.error("[MERCADO PAGO HMAC] ❌ Error inesperado calculando HMAC-SHA256", e);
            return properties.isSandbox();
        }
    }

}

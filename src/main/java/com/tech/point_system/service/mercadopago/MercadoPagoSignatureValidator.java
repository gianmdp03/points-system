package com.tech.point_system.service.mercadopago;

import com.tech.point_system.config.MercadoPagoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class MercadoPagoSignatureValidator {

    private final MercadoPagoProperties properties;
    private static final String HMAC_SHA256 = "HmacSHA256";

    public boolean isValidSignature(String xSignature, String xRequestId, String dataId) {
        String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank() || secret.startsWith("00000000")) {
            log.warn("[MERCADO PAGO SECURITY] webhook-secret no esta configurado con clave real. Omitiendo validacion estricta en entorno dev.");
            return true;
        }

        if (xSignature == null || xSignature.isBlank()) {
            log.warn("[MERCADO PAGO SECURITY] Cabecera x-signature faltante");
            return false;
        }

        try {
            String ts = null;
            String hash = null;

            for (String part : xSignature.split(",")) {
                String[] keyValue = part.trim().split("=", 2);
                if (keyValue.length == 2) {
                    if ("ts".equalsIgnoreCase(keyValue[0])) {
                        ts = keyValue[1];
                    } else if ("v1".equalsIgnoreCase(keyValue[0])) {
                        hash = keyValue[1];
                    }
                }
            }

            if (ts == null || hash == null) {
                log.warn("[MERCADO PAGO SECURITY] Formato invalido de x-signature: {}", xSignature);
                return false;
            }

            String manifest = "id:" + (dataId != null ? dataId : "") + ";request-id:" + (xRequestId != null ? xRequestId : "") + ";ts:" + ts + ";";

            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            String calculatedHash = HexFormat.of().formatHex(rawHmac);

            return calculatedHash.equalsIgnoreCase(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[MERCADO PAGO SECURITY] Error calculando HMAC-SHA256 para x-signature", e);
            return false;
        }
    }
}

package com.tech.point_system.service.mercadopago;

import com.tech.point_system.config.MercadoPagoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercadoPagoSignatureValidatorTest {

    private MercadoPagoProperties properties;
    private MercadoPagoSignatureValidator validator;

    @BeforeEach
    void setUp() {
        properties = new MercadoPagoProperties();
        validator = new MercadoPagoSignatureValidator(properties);
    }

    @Test
    void shouldPassWhenSecretIsNotSet() {
        properties.setWebhookSecret("");
        assertTrue(validator.isValidSignature("ts=123,v1=abc", "req-1", "data-1"));
    }

    @Test
    void shouldPassWhenSecretIsDefaultZeroes() {
        properties.setWebhookSecret("0000000000000000000000000000000000000000000000000000000000000000");
        assertTrue(validator.isValidSignature("ts=123,v1=abc", "req-1", "data-1"));
    }

    @Test
    void shouldValidateCorrectHmacSha256Signature() throws Exception {
        String secret = "my-secret-webhook-key-123456789";
        properties.setWebhookSecret(secret);

        String ts = "1700000000";
        String requestId = "req-uuid-123";
        String dataId = "preapproval-456";

        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expectedHash = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));

        String header = "ts=" + ts + ",v1=" + expectedHash;

        boolean isValid = validator.isValidSignature(header, requestId, dataId);
        assertTrue(isValid);
    }

    @Test
    void shouldRejectInvalidSignature() {
        properties.setWebhookSecret("my-secret-key-123");
        String header = "ts=1700000000,v1=invalidhash1234567890abcdef";

        boolean isValid = validator.isValidSignature(header, "req-1", "data-1");
        assertFalse(isValid);
    }

    @Test
    void shouldRejectNullOrMalformedSignatureHeader() {
        properties.setWebhookSecret("my-secret-key-123");
        assertFalse(validator.isValidSignature(null, "req-1", "data-1"));
        assertFalse(validator.isValidSignature("malformed-header", "req-1", "data-1"));
    }
}

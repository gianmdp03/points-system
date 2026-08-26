package com.tech.point_system.payment.mercadopago;

import com.tech.point_system.config.MercadoPagoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoSignatureValidatorTest {

    @Mock
    private MercadoPagoProperties properties;

    @InjectMocks
    private MercadoPagoSignatureValidator validator;

    private final String secret = "my-secret-key-12345";

    @BeforeEach
    void setUp() {
        when(properties.getWebhookSecret()).thenReturn(secret);
    }

    @Test
    void testValidSignature() throws Exception {
        String dataId = "99887766";
        String xRequestId = "req-uuid-1234";
        String ts = "1700000000";

        String template = String.format("id:%s;request-id:%s;ts:%s;", dataId, xRequestId, ts);
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] rawHmac = mac.doFinal(template.getBytes(StandardCharsets.UTF_8));
        String expectedHash = HexFormat.of().formatHex(rawHmac);

        String xSignature = "ts=" + ts + ",v1=" + expectedHash;

        boolean result = validator.isValidSignature(xSignature, xRequestId, dataId);
        assertTrue(result);
    }

    @Test
    void testInvalidSignature() {
        String dataId = "99887766";
        String xRequestId = "req-uuid-1234";
        String xSignature = "ts=1700000000,v1=invalid_hash_value";

        boolean result = validator.isValidSignature(xSignature, xRequestId, dataId);
        assertFalse(result);
    }

    @Test
    void testMissingSignatureHeader() {
        boolean result = validator.isValidSignature(null, "req-1", "data-1");
        assertFalse(result);
    }
}

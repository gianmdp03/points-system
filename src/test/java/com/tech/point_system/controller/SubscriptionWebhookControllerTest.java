package com.tech.point_system.controller;

import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.payment.PaymentStrategyFactory;
import com.tech.point_system.payment.mercadopago.MercadoPagoSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionWebhookControllerTest {

    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private MercadoPagoSignatureValidator signatureValidator;

    @Mock
    private PaymentStrategy paymentStrategy;

    @InjectMocks
    private SubscriptionWebhookController webhookController;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testHandleWebhook_MercadoPago_ValidSignature_ProcessesAsync() {
        when(signatureValidator.isValidSignature(any(), any(), any())).thenReturn(true);
        when(paymentStrategyFactory.getStrategy(PaymentProvider.MERCADO_PAGO)).thenReturn(paymentStrategy);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json");
        request.setContent("{\"data\":{\"id\":\"123456\"},\"type\":\"payment\"}".getBytes());

        ResponseEntity<Void> response = webhookController.handleWebhook(
                "MERCADO_PAGO",
                "req-1",
                "ts=123,v1=abc",
                "MercadoPago Webhook Agent",
                Map.of("data.id", "123456"),
                request
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(signatureValidator).isValidSignature("ts=123,v1=abc", "req-1", "123456");
    }

    @Test
    void testHandleWebhook_MercadoPago_CaseInsensitiveProvider() {
        when(signatureValidator.isValidSignature(any(), any(), any())).thenReturn(true);
        when(paymentStrategyFactory.getStrategy(PaymentProvider.MERCADO_PAGO)).thenReturn(paymentStrategy);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.addParameter("id", "999888");
        request.addParameter("topic", "merchant_order");

        ResponseEntity<Void> response = webhookController.handleWebhook(
                "mercadopago",
                null,
                null,
                null,
                Map.of("id", "999888", "topic", "merchant_order"),
                request
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentStrategyFactory).getStrategy(PaymentProvider.MERCADO_PAGO);
    }

    @Test
    void testHandleWebhook_InvalidSignature_Returns401() {
        when(signatureValidator.isValidSignature(any(), any(), any())).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");

        ResponseEntity<Void> response = webhookController.handleWebhook(
                "MERCADO_PAGO",
                "req-1",
                "ts=123,v1=fake",
                null,
                Map.of("id", "123"),
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(paymentStrategyFactory, never()).getStrategy(any());
    }

    @Test
    void testHandleWebhook_PingHeadRequest_Returns200Immediately() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("HEAD");

        ResponseEntity<Void> response = webhookController.handleWebhook(
                "MERCADO_PAGO",
                null,
                null,
                null,
                Map.of(),
                request
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verifyNoInteractions(signatureValidator);
        verifyNoInteractions(paymentStrategyFactory);
    }
}

package com.tech.point_system.payment.mercadopago;

import com.tech.point_system.config.MercadoPagoProperties;
import com.tech.point_system.dto.mercadopago.MpPreferenceModels.MpPaymentResponse;
import com.tech.point_system.dto.mercadopago.MpPreferenceModels.MpPreferenceRequest;
import com.tech.point_system.dto.mercadopago.MpPreferenceModels.MpPreferenceResponse;
import com.tech.point_system.exception.PaymentGatewayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class MercadoPagoPreferenceClient {

    private final RestClient restClient;
    private final MercadoPagoProperties properties;

    public MercadoPagoPreferenceClient(MercadoPagoProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public MpPreferenceResponse createPreference(MpPreferenceRequest request) {
        log.info("[CHECKOUT PRO API] 📤 [POST /checkout/preferences] Creando preferencia de pago | ExternalRef='{}' | NotificationUrl='{}' | BackUrl='{}'",
                request.externalReference(), request.notificationUrl(), request.backUrls() != null ? request.backUrls().success() : "N/A");

        if (request.items() != null) {
            request.items().forEach(item ->
                    log.info("[CHECKOUT PRO API] 📦 Ítem: '{}' x{} | Precio Unitario={} {}",
                            item.title(), item.quantity(), item.unitPrice(), item.currencyId())
            );
        }

        try {
            MpPreferenceResponse response = restClient.post()
                    .uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MpPreferenceResponse.class);

            if (response != null) {
                log.info("[CHECKOUT PRO API] 📥 [POST /checkout/preferences - ÉXITO] Preferencia ID='{}' | InitPoint='{}' | SandboxInitPoint='{}'",
                        response.id(), response.initPoint(), response.sandboxInitPoint());
            } else {
                log.warn("[CHECKOUT PRO API] ⚠️ [POST /checkout/preferences] Respuesta nula recibida de Mercado Pago.");
            }

            return response;

        } catch (RestClientResponseException rce) {
            log.error("[CHECKOUT PRO API] ❌ [POST /checkout/preferences - ERROR HTTP] Código: {} {} | Cuerpo de error: {}",
                    rce.getStatusCode(), rce.getStatusText(), rce.getResponseBodyAsString());
            throw new PaymentGatewayException("rejected", "preference_creation_error",
                    "Error creando la preferencia en Mercado Pago (" + rce.getStatusCode() + "): " + rce.getResponseBodyAsString(), 400);
        } catch (Exception e) {
            log.error("[CHECKOUT PRO API] ❌ [POST /checkout/preferences - ERROR DE CONEXIÓN] Mensaje: {}", e.getMessage(), e);
            throw new PaymentGatewayException("rejected", "connection_error",
                    "Error de comunicación con Mercado Pago: " + e.getMessage(), 400);
        }
    }

    public MpPaymentResponse getPayment(String paymentId) {
        log.info("[CHECKOUT PRO API] 🔍 [GET /v1/payments/{}] Consultando pago en Mercado Pago...", paymentId);

        try {
            MpPaymentResponse response = restClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(MpPaymentResponse.class);

            if (response != null) {
                log.info("[CHECKOUT PRO API] 📥 [GET /v1/payments/{}] Estado obtenido: ID={} | Status='{}' | StatusDetail='{}' | Monto={} | ExternalRef='{}'",
                        paymentId, response.id(), response.status(), response.statusDetail(), response.transactionAmount(), response.externalReference());
            } else {
                log.warn("[CHECKOUT PRO API] ⚠️ [GET /v1/payments/{}] Pago no encontrado o respuesta vacía.", paymentId);
            }

            return response;

        } catch (RestClientResponseException rce) {
            log.error("[CHECKOUT PRO API] ❌ [GET /v1/payments/{} - ERROR HTTP] Código: {} {} | Cuerpo: {}",
                    paymentId, rce.getStatusCode(), rce.getStatusText(), rce.getResponseBodyAsString());
            throw new PaymentGatewayException("rejected", "payment_query_error",
                    "Error consultando pago en Mercado Pago (" + rce.getStatusCode() + "): " + rce.getResponseBodyAsString(), 400);
        } catch (Exception e) {
            log.error("[CHECKOUT PRO API] ❌ [GET /v1/payments/{} - ERROR DE CONEXIÓN] Mensaje: {}", paymentId, e.getMessage(), e);
            throw new PaymentGatewayException("rejected", "connection_error",
                    "Error de comunicación con Mercado Pago: " + e.getMessage(), 400);
        }
    }

    public com.tech.point_system.dto.mercadopago.MpPreferenceModels.MpMerchantOrderResponse getMerchantOrder(String orderId) {
        log.info("[CHECKOUT PRO API] 🔍 [GET /merchant_orders/{}] Consultando orden comercial en Mercado Pago...", orderId);

        try {
            com.tech.point_system.dto.mercadopago.MpPreferenceModels.MpMerchantOrderResponse response = restClient.get()
                    .uri("/merchant_orders/{id}", orderId)
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(com.tech.point_system.dto.mercadopago.MpPreferenceModels.MpMerchantOrderResponse.class);

            if (response != null) {
                log.info("[CHECKOUT PRO API] 📥 [GET /merchant_orders/{}] Orden obtenida: ID={} | Status='{}' | PagosAsociados={}",
                        orderId, response.id(), response.status(), response.payments() != null ? response.payments().size() : 0);
            }
            return response;
        } catch (Exception e) {
            log.error("[CHECKOUT PRO API] ❌ [GET /merchant_orders/{}] Error consultando orden comercial: {}", orderId, e.getMessage());
            return null;
        }
    }
}


package com.tech.point_system.service.mercadopago;

import com.tech.point_system.config.MercadoPagoProperties;
import com.tech.point_system.dto.mercadopago.MercadoPagoPreapprovalRequest;
import com.tech.point_system.dto.mercadopago.MercadoPagoPreapprovalResponse;
import com.tech.point_system.dto.mercadopago.MercadoPagoPreapprovalUpdateRequest;
import com.tech.point_system.exception.PaymentGatewayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class MercadoPagoClient {

    private final RestClient restClient;

    @Autowired
    public MercadoPagoClient(MercadoPagoProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public MercadoPagoClient(MercadoPagoProperties properties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public MercadoPagoPreapprovalResponse createPreapproval(MercadoPagoPreapprovalRequest request) {
        log.info("[MERCADO PAGO CLIENT] Creando preapproval para email: {}", request.payerEmail());
        try {
            return restClient.post()
                    .uri("/preapproval")
                    .body(request)
                    .retrieve()
                    .body(MercadoPagoPreapprovalResponse.class);
        } catch (RestClientResponseException ex) {
            log.error("[MERCADO PAGO CLIENT] Error al crear suscripcion: HTTP {} - Body: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new PaymentGatewayException("Error al comunicarse con Mercado Pago: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("[MERCADO PAGO CLIENT] Error inesperado en createPreapproval", ex);
            throw new PaymentGatewayException("Error al conectar con Mercado Pago: " + ex.getMessage(), ex);
        }
    }

    public MercadoPagoPreapprovalResponse getPreapproval(String preapprovalId) {
        log.info("[MERCADO PAGO CLIENT] Consultando preapproval ID: {}", preapprovalId);
        try {
            return restClient.get()
                    .uri("/preapproval/{id}", preapprovalId)
                    .retrieve()
                    .body(MercadoPagoPreapprovalResponse.class);
        } catch (RestClientResponseException ex) {
            log.error("[MERCADO PAGO CLIENT] Error al consultar suscripcion {}: HTTP {} - Body: {}",
                    preapprovalId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new PaymentGatewayException("Error al consultar suscripcion en Mercado Pago: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("[MERCADO PAGO CLIENT] Error inesperado en getPreapproval", ex);
            throw new PaymentGatewayException("Error al conectar con Mercado Pago: " + ex.getMessage(), ex);
        }
    }

    public MercadoPagoPreapprovalResponse updatePreapproval(String preapprovalId, MercadoPagoPreapprovalUpdateRequest request) {
        log.info("[MERCADO PAGO CLIENT] Actualizando preapproval ID: {}", preapprovalId);
        try {
            return restClient.put()
                    .uri("/preapproval/{id}", preapprovalId)
                    .body(request)
                    .retrieve()
                    .body(MercadoPagoPreapprovalResponse.class);
        } catch (RestClientResponseException ex) {
            log.error("[MERCADO PAGO CLIENT] Error al actualizar suscripcion {}: HTTP {} - Body: {}",
                    preapprovalId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new PaymentGatewayException("Error al actualizar suscripcion en Mercado Pago: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("[MERCADO PAGO CLIENT] Error inesperado en updatePreapproval", ex);
            throw new PaymentGatewayException("Error al conectar con Mercado Pago: " + ex.getMessage(), ex);
        }
    }

    public void cancelPreapproval(String preapprovalId) {
        log.info("[MERCADO PAGO CLIENT] Cancelando preapproval ID: {}", preapprovalId);
        updatePreapproval(preapprovalId, new MercadoPagoPreapprovalUpdateRequest(null, null, null, "cancelled"));
    }
}

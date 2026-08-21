package com.tech.point_system.dto.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoWebhookNotification(
        String id,

        @JsonProperty("live_mode")
        Boolean liveMode,

        String type,

        @JsonProperty("date_created")
        String dateCreated,

        @JsonProperty("user_id")
        Long userId,

        @JsonProperty("api_version")
        String apiVersion,

        String action,

        MercadoPagoWebhookData data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MercadoPagoWebhookData(
            String id
    ) {}
}

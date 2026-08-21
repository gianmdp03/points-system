package com.tech.point_system.dto.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MercadoPagoPreapprovalRequest(
        String reason,

        @JsonProperty("payer_email")
        String payerEmail,

        @JsonProperty("auto_recurring")
        MercadoPagoAutoRecurring autoRecurring,

        @JsonProperty("back_url")
        String backUrl,

        @JsonProperty("external_reference")
        String externalReference,

        String status
) {}

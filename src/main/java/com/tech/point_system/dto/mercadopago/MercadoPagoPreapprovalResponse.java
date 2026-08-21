package com.tech.point_system.dto.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoPreapprovalResponse(
        String id,

        @JsonProperty("payer_id")
        Long payerId,

        @JsonProperty("payer_email")
        String payerEmail,

        @JsonProperty("back_url")
        String backUrl,

        @JsonProperty("collector_id")
        Long collectorId,

        @JsonProperty("application_id")
        Long applicationId,

        String status,

        String reason,

        @JsonProperty("external_reference")
        String externalReference,

        @JsonProperty("date_created")
        String dateCreated,

        @JsonProperty("last_modified")
        String lastModified,

        @JsonProperty("init_point")
        String initPoint,

        @JsonProperty("auto_recurring")
        MercadoPagoAutoRecurring autoRecurring,

        @JsonProperty("next_payment_date")
        String nextPaymentDate
) {}

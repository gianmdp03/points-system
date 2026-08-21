package com.tech.point_system.dto.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MercadoPagoAutoRecurring(
        Integer frequency,

        @JsonProperty("frequency_type")
        String frequencyType,

        @JsonProperty("transaction_amount")
        BigDecimal transactionAmount,

        @JsonProperty("currency_id")
        String currencyId,

        @JsonProperty("billing_day")
        Integer billingDay,

        @JsonProperty("billing_day_proportional")
        Boolean billingDayProportional
) {}

package com.tech.point_system.dto.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class MpPreferenceModels {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpPreferenceRequest(
            List<MpPreferenceItem> items,
            MpPreferencePayer payer,
            @JsonProperty("back_urls")
            MpPreferenceBackUrls backUrls,
            @JsonProperty("auto_return")
            String autoReturn,
            @JsonProperty("notification_url")
            String notificationUrl,
            @JsonProperty("external_reference")
            String externalReference,
            @JsonProperty("statement_descriptor")
            String statementDescriptor,
            @JsonProperty("binary_mode")
            Boolean binaryMode
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpPreferenceItem(
            String id,
            String title,
            String description,
            int quantity,
            @JsonProperty("unit_price")
            BigDecimal unitPrice,
            @JsonProperty("currency_id")
            String currencyId
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpPreferencePayer(
            String name,
            String surname,
            String email
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpPreferenceBackUrls(
            String success,
            String pending,
            String failure
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpPreferenceResponse(
            String id,
            @JsonProperty("init_point")
            String initPoint,
            @JsonProperty("sandbox_init_point")
            String sandboxInitPoint,
            @JsonProperty("external_reference")
            String externalReference
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpPaymentResponse(
            Long id,
            String status,
            @JsonProperty("status_detail")
            String statusDetail,
            @JsonProperty("external_reference")
            String externalReference,
            @JsonProperty("transaction_amount")
            BigDecimal transactionAmount,
            @JsonProperty("date_approved")
            String dateApproved,
            @JsonProperty("payment_method_id")
            String paymentMethodId,
            MpPaymentPayer payer
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpPaymentPayer(
            String id,
            String email
    ) {}


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpMerchantOrderResponse(
            Long id,
            String status,
            @JsonProperty("external_reference")
            String externalReference,
            List<MpMerchantOrderPayment> payments
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpMerchantOrderPayment(
            Long id,
            String status,
            @JsonProperty("transaction_amount")
            BigDecimal transactionAmount,
            @JsonProperty("total_paid_amount")
            BigDecimal totalPaidAmount
    ) {}
}


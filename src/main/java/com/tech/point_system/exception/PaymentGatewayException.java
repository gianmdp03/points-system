package com.tech.point_system.exception;

import lombok.Getter;

@Getter
public class PaymentGatewayException extends RuntimeException {
    private final String status;
    private final String statusDetail;
    private final int httpStatus;

    public PaymentGatewayException(String status, String statusDetail, String userMessage, int httpStatus) {
        super(userMessage);
        this.status = status != null ? status : "rejected";
        this.statusDetail = statusDetail != null ? statusDetail : "unknown";
        this.httpStatus = httpStatus > 0 ? httpStatus : 400;
    }

    public PaymentGatewayException(String userMessage) {
        this("rejected", "unknown", userMessage, 400);
    }

    public PaymentGatewayException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.status = "rejected";
        this.statusDetail = "unknown";
        this.httpStatus = 400;
    }
}

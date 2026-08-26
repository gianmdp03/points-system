package com.tech.point_system.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

public record PaymentErrorResponse(
        int code,
        String error,
        String status,
        String statusDetail,
        String message,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime timestamp
) {}

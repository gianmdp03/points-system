package com.tech.point_system.dto.pointsTransaction;

import com.tech.point_system._enum.TransactionType;

import java.time.OffsetDateTime;

public record PointsTransactionDetailDTO(Long id, Integer amount, TransactionType transactionType, OffsetDateTime createdAt) {}

package com.tech.point_system.repository;

import com.tech.point_system._enum.TransactionType;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.model.PointsTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {
    @EntityGraph(attributePaths = {"pointsAccount", "pointsAccount.client", "pointsAccount.company"})
    Page<PointsTransaction> findByPointsAccount(PointsAccount pointsAccount, Pageable pageable);

    List<PointsTransaction> findByPointsAccountIdAndTransactionTypeAndAvailableAmountGreaterThanOrderByCreatedAtAsc(
            Long pointsAccountId, TransactionType transactionType, Integer minAvailableAmount);

    @Query("SELECT pt FROM PointsTransaction pt " +
           "JOIN FETCH pt.pointsAccount pa " +
           "WHERE pt.transactionType = com.tech.point_system._enum.TransactionType.EARNED " +
           "AND pt.expiresAt IS NOT NULL " +
           "AND pt.expiresAt < :now " +
           "AND pt.availableAmount > 0")
    List<PointsTransaction> findExpiredTransactions(@Param("now") OffsetDateTime now);

    @Query("SELECT pt FROM PointsTransaction pt " +
           "JOIN FETCH pt.pointsAccount pa " +
           "JOIN FETCH pa.client c " +
           "JOIN FETCH pa.company comp " +
           "WHERE pt.transactionType = com.tech.point_system._enum.TransactionType.EARNED " +
           "AND pt.availableAmount > 0 " +
           "AND pt.expiresAt IS NOT NULL " +
           "AND pt.expiresAt >= :startDate AND pt.expiresAt < :endDate " +
           "AND comp.isPointsExpirationEnabled = true " +
           "AND c.isNotificationEnabled = true " +
           "AND c.email IS NOT NULL AND TRIM(c.email) != ''")
    List<PointsTransaction> findTransactionsExpiringBetween(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);
}

package com.tech.point_system.task;

import com.tech.point_system._enum.TransactionType;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.model.PointsTransaction;
import com.tech.point_system.repository.PointsAccountRepository;
import com.tech.point_system.repository.PointsTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpirationTasks {

    private final PointsTransactionRepository transactionRepository;
    private final PointsAccountRepository pointsAccountRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void expirePoints() {
        log.info("Iniciando tarea programada: vencimiento de puntos FIFO en Batch...");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<PointsTransaction> expiredTransactions = transactionRepository.findExpiredTransactions(now);

        if (expiredTransactions.isEmpty()) {
            log.debug("No se encontraron transacciones con puntos vencidos en este ciclo.");
            return;
        }

        int count = 0;
        int totalPointsExpired = 0;
        Map<Long, PointsAccount> accountsMap = new HashMap<>();
        List<PointsTransaction> newExpiredTxs = new ArrayList<>();
        List<PointsTransaction> updatedOriginalTxs = new ArrayList<>();

        for (PointsTransaction tx : expiredTransactions) {
            Integer amountToExpire = tx.getAvailableAmount();
            if (amountToExpire == null || amountToExpire <= 0) {
                continue;
            }

            PointsAccount account = tx.getPointsAccount();
            if (account != null) {
                PointsAccount managedAccount = accountsMap.computeIfAbsent(account.getId(), k -> account);
                int currentBalance = managedAccount.getBalance() != null ? managedAccount.getBalance() : 0;
                managedAccount.setBalance(Math.max(0, currentBalance - amountToExpire));

                // Crear transaccion de vencimiento
                PointsTransaction expiredTx = new PointsTransaction();
                expiredTx.setPointsAccount(managedAccount);
                expiredTx.setAmount(-amountToExpire);
                expiredTx.setAvailableAmount(0);
                expiredTx.setTransactionType(TransactionType.EXPIRED);
                expiredTx.setCreatedAt(now);
                expiredTx.setExpiresAt(null);
                newExpiredTxs.add(expiredTx);

                // Resetear availableAmount en la transaccion original
                tx.setAvailableAmount(0);
                updatedOriginalTxs.add(tx);

                count++;
                totalPointsExpired += amountToExpire;
            }
        }

        // Persistir en lote
        if (!accountsMap.isEmpty()) {
            pointsAccountRepository.saveAll(accountsMap.values());
            transactionRepository.saveAll(newExpiredTxs);
            transactionRepository.saveAll(updatedOriginalTxs);
        }

        log.info("Tarea de vencimiento finalizada: se vencieron {} puntos en {} transacciones (consolidadas en {} cuentas).",
                totalPointsExpired, count, accountsMap.size());
    }
}

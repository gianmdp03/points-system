package com.tech.point_system.task;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system._enum.TransactionType;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.model.PointsTransaction;
import com.tech.point_system.repository.PointsTransactionRepository;
import com.tech.point_system.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsExpirationNotificationTasksTest {

    @Mock
    private PointsTransactionRepository transactionRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PointsExpirationNotificationTasks expirationNotificationTasks;

    private Company company;
    private Client client;
    private PointsAccount pointsAccount;
    private PointsTransaction transaction30Days;
    private PointsTransaction transaction10Days;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(10L);
        company.setName("Havanna");
        company.setIsPointsExpirationEnabled(true);
        company.setPointsExpirationDays(90);

        client = new Client();
        client.setId(100L);
        client.setName("Laura Soria");
        client.setEmail("laura@example.com");
        client.setIsNotificationEnabled(true);

        pointsAccount = new PointsAccount();
        pointsAccount.setId(500L);
        pointsAccount.setCompany(company);
        pointsAccount.setClient(client);
        pointsAccount.setBalance(400);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        transaction30Days = new PointsTransaction();
        transaction30Days.setId(1L);
        transaction30Days.setPointsAccount(pointsAccount);
        transaction30Days.setTransactionType(TransactionType.EARNED);
        transaction30Days.setAmount(200);
        transaction30Days.setAvailableAmount(200);
        transaction30Days.setExpiresAt(now.plusDays(30));

        transaction10Days = new PointsTransaction();
        transaction10Days.setId(2L);
        transaction10Days.setPointsAccount(pointsAccount);
        transaction10Days.setTransactionType(TransactionType.EARNED);
        transaction10Days.setAmount(150);
        transaction10Days.setAvailableAmount(150);
        transaction10Days.setExpiresAt(now.plusDays(10));
    }

    @Test
    @DisplayName("sendPointsExpirationAlerts: Despacha alertas para hitos de 30 días y 10 días")
    void sendPointsExpirationAlerts_SendsMilestoneAlerts() {
        when(transactionRepository.findTransactionsExpiringBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenAnswer(invocation -> {
                    OffsetDateTime start = invocation.getArgument(0);
                    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                    if (start.isAfter(now.plusDays(20))) {
                        return List.of(transaction30Days);
                    } else {
                        return List.of(transaction10Days);
                    }
                });

        expirationNotificationTasks.sendPointsExpirationAlerts();

        verify(emailService).sendNotificationEmail(
                eq(NotificationType.POINTS_EXPIRATION_NOTIFICATION),
                eq(company),
                eq(client),
                argThat(params -> Integer.valueOf(30).equals(params.get("expirationDays")) && Integer.valueOf(200).equals(params.get("pointsExpiring")))
        );

        verify(emailService).sendNotificationEmail(
                eq(NotificationType.POINTS_EXPIRATION_NOTIFICATION),
                eq(company),
                eq(client),
                argThat(params -> Integer.valueOf(10).equals(params.get("expirationDays")) && Integer.valueOf(150).equals(params.get("pointsExpiring")))
        );
    }
}

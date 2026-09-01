package com.tech.point_system.task;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.PointsAccountRepository;
import com.tech.point_system.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerRetentionTasksTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PointsAccountRepository pointsAccountRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private CustomerRetentionTasks customerRetentionTasks;

    private Company company;
    private Client client;
    private PointsAccount pointsAccount;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(10L);
        company.setName("Heladería Freddo");
        company.setIsClientRetentionEnabled(true);
        company.setClientRetentionDays(20);

        client = new Client();
        client.setId(100L);
        client.setName("Martín Gómez");
        client.setEmail("martin@example.com");
        client.setIsNotificationEnabled(true);

        pointsAccount = new PointsAccount();
        pointsAccount.setId(500L);
        pointsAccount.setCompany(company);
        pointsAccount.setClient(client);
        pointsAccount.setBalance(350);
        pointsAccount.setLastActivityDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(25));
    }

    @Test
    @DisplayName("sendCustomerRetentionNotifications: Despacha email y actualiza lastRetentionNotificationDate para cuentas elegibles")
    void sendCustomerRetentionNotifications_SendsEmailAndUpdatesAccount() {
        when(companyRepository.findByIsClientRetentionEnabledTrueAndClientRetentionDaysIsNotNull())
                .thenReturn(List.of(company));

        when(pointsAccountRepository.findEligibleForRetentionNotification(eq(10L), any(OffsetDateTime.class)))
                .thenReturn(List.of(pointsAccount));

        customerRetentionTasks.sendCustomerRetentionNotifications();

        verify(emailService).sendNotificationEmail(
                eq(NotificationType.CLIENT_RETENTION_NOTIFICATION),
                eq(company),
                eq(client),
                argThat(params -> "350".equals(String.valueOf(params.get("pointsBalance"))) || Integer.valueOf(350).equals(params.get("pointsBalance")))
        );

        assertNotNull(pointsAccount.getLastRetentionNotificationDate());
        verify(pointsAccountRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("sendCustomerRetentionNotifications: No hace nada si no hay empresas con retención activa")
    void sendCustomerRetentionNotifications_NoCompanies_DoesNothing() {
        when(companyRepository.findByIsClientRetentionEnabledTrueAndClientRetentionDaysIsNotNull())
                .thenReturn(List.of());

        customerRetentionTasks.sendCustomerRetentionNotifications();

        verifyNoInteractions(pointsAccountRepository);
        verifyNoInteractions(emailService);
    }
}

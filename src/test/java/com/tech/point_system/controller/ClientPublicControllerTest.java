package com.tech.point_system.controller;

import com.tech.point_system.dto.client.ClientNotificationToggleDTO;
import com.tech.point_system.dto.company.CompanyPublicDetailDTO;
import com.tech.point_system.mapper.PointsAccountMapper;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.ClientRepository;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.PointsAccountRepository;
import com.tech.point_system.service.CompanyService;
import com.tech.point_system.service.PlanValidatorService;
import com.tech.point_system.service.PublicCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientPublicControllerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyService companyService;

    @Mock
    private PointsAccountRepository pointsAccountRepository;

    @Mock
    private PublicCatalogService publicCatalogService;

    @Mock
    private PlanValidatorService planValidatorService;

    @Mock
    private PointsAccountMapper pointsAccountMapper;

    @InjectMocks
    private ClientPublicController clientPublicController;

    private Client client;
    private Company company;
    private PointsAccount pointsAccount;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);
        client.setDni("12345678");
        client.setCountry("Argentina");
        client.setName("Juan Perez");
        client.setEmail("juan@test.com");
        client.setIsNotificationEnabled(true);

        company = new Company();
        company.setId(10L);
        company.setName("Café Central");
        company.setIsEnabled(true);
        company.setAmountStep(new BigDecimal("100"));
        company.setPointsPerStep(1);

        pointsAccount = new PointsAccount();
        pointsAccount.setId(100L);
        pointsAccount.setClient(client);
        pointsAccount.setCompany(company);
        pointsAccount.setBalance(500);
    }

    @Test
    void testGetCompanyPublicDetail_IncludesNotificationStatus() {
        when(clientRepository.findByDniAndCountry("12345678", "Argentina")).thenReturn(Optional.of(client));
        when(pointsAccountRepository.findByClientIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(pointsAccount));
        when(publicCatalogService.getPublicProducts(10L)).thenReturn(List.of());
        when(publicCatalogService.getPublicRewards(10L)).thenReturn(List.of());
        when(publicCatalogService.getPublicPromotions(10L)).thenReturn(List.of());

        ResponseEntity<CompanyPublicDetailDTO> response = clientPublicController.getCompanyPublicDetail("Argentina", "12345678", 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Café Central", response.getBody().name());
        assertEquals(500, response.getBody().clientBalance());
        assertTrue(response.getBody().isNotificationEnabled());
    }

    @Test
    void testUpdateClientNotificationPreference_Success() {
        when(clientRepository.findByDniAndCountry("12345678", "Argentina")).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));

        ClientNotificationToggleDTO dto = new ClientNotificationToggleDTO(false);
        ResponseEntity<Void> response = clientPublicController.updateClientNotificationPreference("Argentina", "12345678", dto);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertFalse(client.getIsNotificationEnabled());
        verify(clientRepository).save(client);
    }

    @Test
    void testJoinCompany_PassesNotificationPreference() {
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        User admin = new User();
        admin.setId("admin-1");
        company.setAdmin(admin);

        when(pointsAccountRepository.countByCompanyId(10L)).thenReturn(5L);
        when(clientRepository.getOrCreateClient("12345678", "Argentina", "Juan Perez", "juan@test.com", "123456", false))
                .thenReturn(client);
        when(pointsAccountRepository.findByClientIdAndCompanyId(1L, 10L)).thenReturn(Optional.empty());
        when(pointsAccountRepository.save(any(PointsAccount.class))).thenAnswer(i -> i.getArgument(0));

        com.tech.point_system.dto.client.ClientJoinRequestDTO joinDTO = new com.tech.point_system.dto.client.ClientJoinRequestDTO(
                10L, "12345678", "Argentina", "Juan Perez", "juan@test.com", "123456", false);

        ResponseEntity<com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO> response = clientPublicController.joinCompany(joinDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(clientRepository).getOrCreateClient("12345678", "Argentina", "Juan Perez", "juan@test.com", "123456", false);
    }
}

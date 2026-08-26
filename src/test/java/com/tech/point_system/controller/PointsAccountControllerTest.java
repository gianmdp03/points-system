package com.tech.point_system.controller;

import com.tech.point_system.dto.client.ClientDetailDTO;
import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import com.tech.point_system.service.PointsAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsAccountControllerTest {

    @Mock
    private PointsAccountService pointsAccountService;

    @InjectMocks
    private PointsAccountController pointsAccountController;

    private Jwt jwt;
    private PointsAccountDetailDTO detailDTO;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject("usr-admin-1")
                .build();

        ClientDetailDTO clientDTO = new ClientDetailDTO(
                1L, "12345678", "Argentina", "Juan Perez", "juan@test.com", "123456", true);
        CompanyListDTO companyDTO = new CompanyListDTO(
                10L, "Mi Comercio", null, null, null, true, null, false, null, false, null);

        detailDTO = new PointsAccountDetailDTO(
                100L, 250, OffsetDateTime.now(ZoneOffset.UTC).minusDays(40), companyDTO, clientDTO);
    }

    @Test
    void testRegisterClientAndCreateAccount() {
        PointsAccountRequestDTO requestDTO = new PointsAccountRequestDTO(
                10L, "12345678", "Argentina", "Juan Perez", "juan@test.com", "123456", true);

        when(pointsAccountService.registerClientAndCreateAccount("usr-admin-1", requestDTO)).thenReturn(detailDTO);

        ResponseEntity<PointsAccountDetailDTO> response = pointsAccountController.registerClientAndCreateAccount(jwt, requestDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(detailDTO, response.getBody());
        verify(pointsAccountService).registerClientAndCreateAccount("usr-admin-1", requestDTO);
    }

    @Test
    void testListInactiveClients() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PointsAccountDetailDTO> page = new PageImpl<>(List.of(detailDTO));

        when(pointsAccountService.listInactiveClients("usr-admin-1", 10L, 30, pageable)).thenReturn(page);

        ResponseEntity<Page<PointsAccountDetailDTO>> response = pointsAccountController.listInactiveClients(jwt, 10L, 30, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(detailDTO, response.getBody().getContent().get(0));
        verify(pointsAccountService).listInactiveClients("usr-admin-1", 10L, 30, pageable);
    }
}
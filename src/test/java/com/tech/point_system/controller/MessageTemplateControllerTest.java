package com.tech.point_system.controller;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateDetailDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateListDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateRequestDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateUpdateDTO;
import com.tech.point_system.service.MessageTemplateService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageTemplateControllerTest {

    @Mock
    private MessageTemplateService messageTemplateService;

    @InjectMocks
    private MessageTemplateController messageTemplateController;

    private Jwt jwt;
    private MessageTemplateDetailDTO detailDTO;
    private MessageTemplateListDTO listDTO;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("usr-admin-1")
                .build();

        CompanyListDTO companyDTO = new CompanyListDTO(
                10L, "Mi Tienda", null, null, null, true, null, false, null, false, null);

        detailDTO = new MessageTemplateDetailDTO(
                1L, "Bienvenida", NotificationType.WELCOME_NOTIFICATION, "¡Bienvenido a {empresa}!",
                "Bienvenido a {empresa}, {nombre}!, muchas gracias por unirte a nosotros!", true, companyDTO);

        listDTO = new MessageTemplateListDTO(
                1L, "Bienvenida", NotificationType.WELCOME_NOTIFICATION, "¡Bienvenido a {empresa}!",
                "Bienvenido a {empresa}, {nombre}!, muchas gracias por unirte a nosotros!", true);
    }

    @Test
    void testAddTemplate() {
        MessageTemplateRequestDTO requestDTO = new MessageTemplateRequestDTO(
                "Bienvenida", NotificationType.WELCOME_NOTIFICATION, "¡Bienvenido a {empresa}!",
                "Bienvenido a {empresa}, {nombre}!, muchas gracias por unirte a nosotros!", 10L);

        when(messageTemplateService.addTemplate("usr-admin-1", requestDTO)).thenReturn(detailDTO);

        ResponseEntity<MessageTemplateDetailDTO> response = messageTemplateController.addTemplate(jwt, requestDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(detailDTO, response.getBody());
        verify(messageTemplateService).addTemplate("usr-admin-1", requestDTO);
    }

    @Test
    void testUpdateTemplate() {
        MessageTemplateUpdateDTO updateDTO = new MessageTemplateUpdateDTO(
                "Bienvenida Actualizada", NotificationType.WELCOME_NOTIFICATION, "Nuevo asunto", "Nuevo contenido");

        when(messageTemplateService.updateTemplate("usr-admin-1", 10L, 1L, updateDTO)).thenReturn(detailDTO);

        ResponseEntity<MessageTemplateDetailDTO> response = messageTemplateController.updateTemplate(jwt, 10L, 1L, updateDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detailDTO, response.getBody());
        verify(messageTemplateService).updateTemplate("usr-admin-1", 10L, 1L, updateDTO);
    }

    @Test
    void testListTemplates() {
        Pageable pageable = PageRequest.of(0, 12);
        Page<MessageTemplateListDTO> page = new PageImpl<>(List.of(listDTO));

        when(messageTemplateService.listTemplates("usr-admin-1", 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<MessageTemplateListDTO>> response = messageTemplateController.listTemplates(jwt, 10L, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalElements());
        verify(messageTemplateService).listTemplates("usr-admin-1", 10L, pageable);
    }

    @Test
    void testGetTemplateById() {
        when(messageTemplateService.getTemplateById("usr-admin-1", 10L, 1L)).thenReturn(detailDTO);

        ResponseEntity<MessageTemplateDetailDTO> response = messageTemplateController.getTemplateById(jwt, 10L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detailDTO, response.getBody());
        verify(messageTemplateService).getTemplateById("usr-admin-1", 10L, 1L);
    }

    @Test
    void testEnableOrDisableTemplate() {
        doNothing().when(messageTemplateService).enableOrDisableTemplate("usr-admin-1", 10L, 1L);

        ResponseEntity<Void> response = messageTemplateController.enableOrDisableTemplate(jwt, 10L, 1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(messageTemplateService).enableOrDisableTemplate("usr-admin-1", 10L, 1L);
    }
}
package com.tech.point_system.service;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.dto.messageTemplate.MessageTemplateDetailDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateListDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateRequestDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateUpdateDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.MessageTemplateMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.MessageTemplate;
import com.tech.point_system.repository.MessageTemplateRepository;
import com.tech.point_system.service.impl.MessageTemplateServiceImpl;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageTemplateServiceImplTest {

    @Mock
    private MessageTemplateRepository messageTemplateRepository;

    @Mock
    private MessageTemplateMapper messageTemplateMapper;

    @Mock
    private CompanyAccessValidator companyAccessValidator;

    @InjectMocks
    private MessageTemplateServiceImpl messageTemplateService;

    private Company company;
    private MessageTemplate template;
    private MessageTemplateDetailDTO detailDTO;
    private MessageTemplateListDTO listDTO;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(10L);
        company.setName("Mi Comercio");

        template = new MessageTemplate();
        template.setId(1L);
        template.setName("Retención");
        template.setType(NotificationType.CLIENT_RETENTION_NOTIFICATION);
        template.setSubject("¡Te extrañamos, {nombre}!");
        template.setContent("¡Te extrañamos, {nombre}! Tenés {puntos} puntos esperándote.");
        template.setIsEnabled(true);
        template.setCompany(company);

        detailDTO = new MessageTemplateDetailDTO(
                1L, "Retención", NotificationType.CLIENT_RETENTION_NOTIFICATION,
                "¡Te extrañamos, {nombre}!", "¡Te extrañamos, {nombre}! Tenés {puntos} puntos esperándote.",
                true, null);

        listDTO = new MessageTemplateListDTO(
                1L, "Retención", NotificationType.CLIENT_RETENTION_NOTIFICATION,
                "¡Te extrañamos, {nombre}!", "¡Te extrañamos, {nombre}! Tenés {puntos} puntos esperándote.",
                true);
    }

    @Test
    void testAddTemplate_Success() {
        MessageTemplateRequestDTO requestDTO = new MessageTemplateRequestDTO(
                "Retención", NotificationType.CLIENT_RETENTION_NOTIFICATION,
                "¡Te extrañamos, {nombre}!", "¡Te extrañamos, {nombre}! Tenés {puntos} puntos esperándote.", 10L);

        when(companyAccessValidator.validateAccess(10L, "usr-admin-1")).thenReturn(company);
        when(messageTemplateMapper.toEntity(requestDTO)).thenReturn(template);
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(template);
        when(messageTemplateMapper.toDetailDTO(template)).thenReturn(detailDTO);

        MessageTemplateDetailDTO result = messageTemplateService.addTemplate("usr-admin-1", requestDTO);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Retención", result.name());
        verify(companyAccessValidator).validateAccess(10L, "usr-admin-1");
        verify(messageTemplateRepository).save(template);
    }

    @Test
    void testUpdateTemplate_Success() {
        MessageTemplateUpdateDTO updateDTO = new MessageTemplateUpdateDTO(
                "Nombre Modificado", NotificationType.CLIENT_RETENTION_NOTIFICATION, "Nuevo Asunto", "Nuevo Contenido");

        when(companyAccessValidator.validateAccess(10L, "usr-admin-1")).thenReturn(company);
        when(messageTemplateRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(template));
        doAnswer(i -> {
            template.setName("Nombre Modificado");
            return null;
        }).when(messageTemplateMapper).updateEntityFromDTO(updateDTO, template);
        when(messageTemplateRepository.save(template)).thenReturn(template);
        when(messageTemplateMapper.toDetailDTO(template)).thenReturn(detailDTO);

        MessageTemplateDetailDTO result = messageTemplateService.updateTemplate("usr-admin-1", 10L, 1L, updateDTO);

        assertNotNull(result);
        verify(companyAccessValidator).validateAccess(10L, "usr-admin-1");
        verify(messageTemplateRepository).save(template);
    }

    @Test
    void testUpdateTemplate_NotFound() {
        MessageTemplateUpdateDTO updateDTO = new MessageTemplateUpdateDTO(
                "Nombre Modificado", NotificationType.CLIENT_RETENTION_NOTIFICATION, "Nuevo Asunto", "Nuevo Contenido");

        when(companyAccessValidator.validateAccess(10L, "usr-admin-1")).thenReturn(company);
        when(messageTemplateRepository.findByIdAndCompanyId(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                messageTemplateService.updateTemplate("usr-admin-1", 10L, 99L, updateDTO));
    }

    @Test
    void testListTemplates_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MessageTemplate> page = new PageImpl<>(List.of(template));

        when(companyAccessValidator.validateAccess(10L, "usr-admin-1")).thenReturn(company);
        when(messageTemplateRepository.countByCompanyId(10L)).thenReturn(1L);
        when(messageTemplateRepository.findByCompanyId(10L, pageable)).thenReturn(page);
        when(messageTemplateMapper.toListDTO(template)).thenReturn(listDTO);

        Page<MessageTemplateListDTO> result = messageTemplateService.listTemplates("usr-admin-1", 10L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(companyAccessValidator).validateAccess(10L, "usr-admin-1");
    }

    @Test
    void testListTemplates_AutoSeedWhenEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MessageTemplate> page = new PageImpl<>(List.of(template));

        when(companyAccessValidator.validateAccess(10L, "usr-admin-1")).thenReturn(company);
        when(messageTemplateRepository.countByCompanyId(10L)).thenReturn(0L);
        when(messageTemplateRepository.findByCompanyId(10L, pageable)).thenReturn(page);
        when(messageTemplateMapper.toListDTO(template)).thenReturn(listDTO);

        Page<MessageTemplateListDTO> result = messageTemplateService.listTemplates("usr-admin-1", 10L, pageable);

        assertNotNull(result);
        verify(messageTemplateRepository).saveAll(any());
    }

    @Test
    void testGetTemplateById_Success() {
        when(messageTemplateRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(template));
        when(messageTemplateMapper.toDetailDTO(template)).thenReturn(detailDTO);

        MessageTemplateDetailDTO result = messageTemplateService.getTemplateById("usr-admin-1", 10L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(companyAccessValidator).checkAccessOnly(10L, "usr-admin-1");
    }

    @Test
    void testEnableOrDisableTemplate_Success() {
        when(messageTemplateRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(template));
        when(messageTemplateRepository.save(template)).thenReturn(template);

        messageTemplateService.enableOrDisableTemplate("usr-admin-1", 10L, 1L);

        assertFalse(template.getIsEnabled());
        verify(companyAccessValidator).checkAccessOnly(10L, "usr-admin-1");
        verify(messageTemplateRepository).save(template);
    }

    @Test
    void testResetDefaultTemplates_Success() {
        when(companyAccessValidator.validateAccess(10L, "usr-admin-1")).thenReturn(company);
        when(messageTemplateRepository.findByCompanyId(10L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(template)));
        when(messageTemplateRepository.saveAll(any())).thenReturn(List.of(template));
        when(messageTemplateMapper.toDetailDTO(any(MessageTemplate.class))).thenReturn(detailDTO);

        List<MessageTemplateDetailDTO> result = messageTemplateService.resetDefaultTemplates("usr-admin-1", 10L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(messageTemplateRepository).deleteAll(any());
        verify(messageTemplateRepository).saveAll(any());
    }
}
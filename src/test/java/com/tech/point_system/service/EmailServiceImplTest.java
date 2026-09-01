package com.tech.point_system.service;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.extra.CompanyDetails;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.MessageTemplate;
import com.tech.point_system.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MessageTemplateService messageTemplateService;

    @InjectMocks
    private EmailServiceImpl emailService;

    private Company company;
    private Client client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "email", "noreply@pointly.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:4200");

        company = new Company();
        company.setId(10L);
        company.setName("Café Martínez");
        company.setCompanyDetails(new CompanyDetails("Argentina", "Buenos Aires", "CABA", "Av. Santa Fe 1234", "1425"));

        client = new Client();
        client.setId(100L);
        client.setName("Juan Pérez");
        client.setEmail("juan.perez@example.com");
        client.setIsNotificationEnabled(true);
    }

    @Test
    @DisplayName("sendNotificationEmail: Envío exitoso con plantilla personalizada e interpolación de variables")
    void sendNotificationEmail_WithCustomTemplate_Success() {
        MessageTemplate customTemplate = new MessageTemplate();
        customTemplate.setId(1L);
        customTemplate.setType(NotificationType.WELCOME_NOTIFICATION);
        customTemplate.setSubject("¡Hola {nombre}, bienvenido a {empresa}!");
        customTemplate.setContent("Gracias {nombre} por visitarnos en {local}. Tenés {puntos} puntos.");

        when(messageTemplateService.getRandomActiveTemplate(10L, NotificationType.WELCOME_NOTIFICATION))
                .thenReturn(Optional.of(customTemplate));

        when(templateEngine.process(eq("welcome-notification"), any(Context.class)))
                .thenReturn("<html>Bienvenido Juan</html>");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("pointsBalance", 150);
        extraParams.put("localName", "Sucursal Recoleta");

        emailService.sendNotificationEmail(NotificationType.WELCOME_NOTIFICATION, company, client, extraParams);

        verify(messageTemplateService).getRandomActiveTemplate(10L, NotificationType.WELCOME_NOTIFICATION);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("welcome-notification"), contextCaptor.capture());

        Context capturedContext = contextCaptor.getValue();
        assertEquals("Café Martínez", capturedContext.getVariable("companyName"));
        assertEquals("Juan Pérez", capturedContext.getVariable("clientName"));
        assertEquals("150", capturedContext.getVariable("pointsBalance"));
        assertEquals("Sucursal Recoleta", capturedContext.getVariable("localName"));
        assertEquals("Av. Santa Fe 1234", capturedContext.getVariable("companyAddress"));
        assertEquals("http://localhost:4200/client-points", capturedContext.getVariable("portalUrl"));

        @SuppressWarnings("unchecked")
        Map<String, String> tmplMap = (Map<String, String>) capturedContext.getVariable("template");
        assertNotNull(tmplMap);
        assertEquals("¡Hola Juan Pérez, bienvenido a Café Martínez!", tmplMap.get("subject"));
        assertEquals("Gracias Juan Pérez por visitarnos en Sucursal Recoleta. Tenés 150 puntos.", tmplMap.get("content"));

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendNotificationEmail: Envío exitoso con fallback por defecto cuando no hay plantilla en BD")
    void sendNotificationEmail_WithoutCustomTemplate_UsesDefaultFallback() {
        when(messageTemplateService.getRandomActiveTemplate(10L, NotificationType.ALMOST_THERE_NOTIFICATION))
                .thenReturn(Optional.empty());

        when(templateEngine.process(eq("almost-there-notification"), any(Context.class)))
                .thenReturn("<html>Estás cerca</html>");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("pointsMissing", 50);

        emailService.sendNotificationEmail(NotificationType.ALMOST_THERE_NOTIFICATION, company, client, extraParams);

        verify(templateEngine).process(eq("almost-there-notification"), any(Context.class));
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendNotificationEmail: Omite el envío si el cliente tiene notificaciones desactivadas")
    void sendNotificationEmail_NotificationsDisabled_SkipsSending() {
        client.setIsNotificationEnabled(false);

        emailService.sendNotificationEmail(NotificationType.CLIENT_RETENTION_NOTIFICATION, company, client, Map.of());

        verifyNoInteractions(messageTemplateService);
        verifyNoInteractions(templateEngine);
        verifyNoInteractions(javaMailSender);
    }

    @Test
    @DisplayName("sendNotificationEmail: Omite el envío si el cliente no tiene email o es nulo")
    void sendNotificationEmail_NullOrEmptyEmail_SkipsSending() {
        client.setEmail(null);

        emailService.sendNotificationEmail(NotificationType.PROMOTION_NOTIFICATION, company, client, Map.of());

        client.setEmail("   ");
        emailService.sendNotificationEmail(NotificationType.PROMOTION_NOTIFICATION, company, client, Map.of());

        verifyNoInteractions(messageTemplateService);
        verifyNoInteractions(templateEngine);
        verifyNoInteractions(javaMailSender);
    }

    @Test
    @DisplayName("sendNotificationEmail: Omite el envío si el cliente es nulo")
    void sendNotificationEmail_NullClient_SkipsSending() {
        emailService.sendNotificationEmail(NotificationType.POINTS_EXPIRATION_NOTIFICATION, company, null, Map.of());

        verifyNoInteractions(messageTemplateService);
        verifyNoInteractions(templateEngine);
        verifyNoInteractions(javaMailSender);
    }

    @Test
    @DisplayName("sendNotificationEmail: Mapeo correcto para todos los tipos de notificación")
    void sendNotificationEmail_AllNotificationTypes_ResolveCorrectTemplates() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Contenido</html>");

        emailService.sendNotificationEmail(NotificationType.WELCOME_NOTIFICATION, company, client, null);
        verify(templateEngine).process(eq("welcome-notification"), any(Context.class));

        emailService.sendNotificationEmail(NotificationType.ALMOST_THERE_NOTIFICATION, company, client, null);
        verify(templateEngine).process(eq("almost-there-notification"), any(Context.class));

        emailService.sendNotificationEmail(NotificationType.CLIENT_RETENTION_NOTIFICATION, company, client, null);
        verify(templateEngine).process(eq("client-retention-notification"), any(Context.class));

        emailService.sendNotificationEmail(NotificationType.POINTS_EXPIRATION_NOTIFICATION, company, client, null);
        verify(templateEngine).process(eq("points-expiration-notification"), any(Context.class));

        emailService.sendNotificationEmail(NotificationType.PROMOTION_NOTIFICATION, company, client, null);
        verify(templateEngine).process(eq("promotion-notification"), any(Context.class));

        emailService.sendNotificationEmail(NotificationType.CUSTOM_NOTIFICATION, company, client, null);
        verify(templateEngine).process(eq("custom-notification"), any(Context.class));
    }

    @Test
    @DisplayName("sendNotificationEmail: Captura y maneja excepciones sin propagar error")
    void sendNotificationEmail_ExceptionHandledGracefully() {
        when(templateEngine.process(anyString(), any(Context.class))).thenThrow(new RuntimeException("Thymeleaf parsing error"));

        assertDoesNotThrow(() ->
                emailService.sendNotificationEmail(NotificationType.WELCOME_NOTIFICATION, company, client, null)
        );
    }
}


package com.tech.point_system.service.impl;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.MessageTemplate;
import com.tech.point_system.service.EmailService;
import com.tech.point_system.service.MessageTemplateService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final MessageTemplateService messageTemplateService;

    @Value("${spring.mail.username:noreply@pointly.com}")
    private String email;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    @Async
    public void sendNotificationEmail(
            NotificationType type, Company company, Client client, Map<String, Object> extraParams) {
        if (!shouldSendEmail(type, client)) {
            return;
        }

        try {
            Map<String, String> replacements = buildReplacements(company, client, extraParams);
            ResolvedTemplateContent templateContent = resolveTemplateContent(type, company, replacements);
            Map<String, Object> variables = buildThymeleafVariables(company, client, templateContent, replacements, extraParams);
            String templateName = resolveTemplateName(type);

            renderAndSendMimeEmail(client.getEmail().trim(), templateContent.subject(), templateName, variables);
        } catch (Exception e) {
            log.error("[EMAIL SERVICE] Error al procesar notificación de correo tipo {} para '{}'",
                    type, client != null ? client.getEmail() : "desconocido", e);
        }
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private boolean shouldSendEmail(NotificationType type, Client client) {
        if (client == null) {
            log.warn("[EMAIL SERVICE] No se puede enviar email: cliente nulo.");
            return false;
        }

        String clientEmail = client.getEmail();
        if (clientEmail == null || clientEmail.trim().isEmpty()) {
            log.info("[EMAIL SERVICE] Omitiendo envío de correo {}: el cliente '{}' (ID: {}) no tiene email registrado.",
                    type, client.getName(), client.getId());
            return false;
        }

        if (!Boolean.TRUE.equals(client.getIsNotificationEnabled())) {
            log.info("[EMAIL SERVICE] Notificaciones deshabilitadas para el cliente '{}' (ID: {}). Omitiendo envío de correo tipo {}.",
                    client.getName(), client.getId(), type);
            return false;
        }

        if (type == null) {
            log.warn("[EMAIL SERVICE] Tipo de notificación nulo al intentar enviar email a '{}'.", clientEmail);
            return false;
        }

        return true;
    }

    private Map<String, String> buildReplacements(
            Company company, Client client, Map<String, Object> extraParams) {
        String companyName = (company != null && company.getName() != null) ? company.getName() : "Pointly";
        String clientName = (client != null && client.getName() != null) ? client.getName() : "Cliente";
        String localName = extractParam(extraParams, companyName, "localName", "local");
        String pointsStr = extractParam(extraParams, "0", "pointsBalance", "currentPoints", "puntos", "points");
        String pointsMissingStr = extractParam(extraParams, "", "pointsMissing", "puntos_faltantes");
        String daysStr = extractParam(extraParams, "", "expirationDays", "dias");

        Map<String, String> replacements = new HashMap<>();
        replacements.put("empresa", companyName);
        replacements.put("local", localName);
        replacements.put("nombre", clientName);
        replacements.put("puntos", pointsStr);
        replacements.put("puntos_faltantes", pointsMissingStr);
        replacements.put("dias", daysStr);
        return replacements;
    }

    private String extractParam(Map<String, Object> extraParams, String defaultValue, String... keys) {
        if (extraParams != null) {
            for (String key : keys) {
                Object value = extraParams.get(key);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        }
        return defaultValue;
    }

    private record ResolvedTemplateContent(String subject, String content, Map<String, String> templateContext) {}

    private ResolvedTemplateContent resolveTemplateContent(
            NotificationType type, Company company, Map<String, String> replacements) {
        Optional<MessageTemplate> templateOpt = (company != null && company.getId() != null)
                ? messageTemplateService.getRandomActiveTemplate(company.getId(), type)
                : Optional.empty();

        if (templateOpt.isPresent()) {
            MessageTemplate tmpl = templateOpt.get();
            String subject = interpolatePlaceholders(tmpl.getSubject(), replacements);
            String content = interpolatePlaceholders(tmpl.getContent(), replacements);

            Map<String, String> templateContext = new HashMap<>();
            templateContext.put("subject", subject);
            templateContext.put("content", content);
            return new ResolvedTemplateContent(subject, content, templateContext);
        }

        String fallbackSubject = resolveDefaultSubject(
                type, replacements.get("empresa"), replacements.get("nombre"), replacements.get("local"));
        return new ResolvedTemplateContent(fallbackSubject, null, null);
    }

    private Map<String, Object> buildThymeleafVariables(
            Company company,
            Client client,
            ResolvedTemplateContent templateContent,
            Map<String, String> replacements,
            Map<String, Object> extraParams) {

        Map<String, Object> variables = new HashMap<>();
        if (extraParams != null) {
            variables.putAll(extraParams);
        }

        String companyName = replacements.get("empresa");
        String clientName = replacements.get("nombre");
        String localName = replacements.get("local");
        String pointsStr = replacements.get("puntos");
        String pointsMissingStr = replacements.get("puntos_faltantes");
        String daysStr = replacements.get("dias");

        variables.put("template", templateContent.templateContext());
        variables.put("company", company);
        variables.put("companyName", companyName);
        variables.put("empresa", companyName);
        variables.put("client", client);
        variables.put("clientName", clientName);
        variables.put("nombre", clientName);
        variables.put("localName", localName);
        variables.put("local", localName);
        variables.put("subject", templateContent.subject());

        if (templateContent.content() != null) {
            variables.put("content", templateContent.content());
            variables.put("messageContent", templateContent.content());
        }

        variables.put("pointsBalance", pointsStr);
        variables.put("currentPoints", pointsStr);
        variables.put("puntos", pointsStr);
        variables.put("pointsMissing", pointsMissingStr);
        variables.put("puntos_faltantes", pointsMissingStr);
        variables.put("expirationDays", daysStr);
        variables.put("dias", daysStr);

        variables.putIfAbsent("portalUrl", frontendUrl + "/client-points");

        if (!variables.containsKey("companyAddress") || variables.get("companyAddress") == null) {
            String address = (company != null && company.getCompanyDetails() != null && company.getCompanyDetails().address() != null)
                    ? company.getCompanyDetails().address()
                    : "";
            variables.put("companyAddress", address);
        }

        return variables;
    }

    private void renderAndSendMimeEmail(
            String to, String subject, String templateName, Map<String, Object> variables) {
        if (to == null || to.trim().isEmpty()) {
            log.warn("[EMAIL SERVICE] Destinatario de correo vacío o nulo. Omitiendo envío.");
            return;
        }

        try {
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }

            String normalizedTemplate = (templateName != null && templateName.endsWith(".html"))
                    ? templateName.substring(0, templateName.length() - 5)
                    : templateName;

            String htmlContent = templateEngine.process(normalizedTemplate, context);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(email);
            helper.setTo(to.trim());
            helper.setSubject(subject != null ? subject : "Notificación de Pointly");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("[EMAIL SERVICE] Email HTML enviado exitosamente a: '{}' con plantilla: '{}'", to, normalizedTemplate);
        } catch (Exception e) {
            log.error("[EMAIL SERVICE] Fallo al procesar/enviar el correo HTML basado en plantilla '{}' a '{}'", templateName, to, e);
        }
    }

    private String resolveTemplateName(NotificationType type) {
        return switch (type) {
            case WELCOME_NOTIFICATION -> "welcome-notification";
            case ALMOST_THERE_NOTIFICATION -> "almost-there-notification";
            case CLIENT_RETENTION_NOTIFICATION -> "client-retention-notification";
            case POINTS_EXPIRATION_NOTIFICATION -> "points-expiration-notification";
            case PROMOTION_NOTIFICATION -> "promotion-notification";
            case CUSTOM_NOTIFICATION -> "custom-notification";
        };
    }

    private String resolveDefaultSubject(
            NotificationType type, String companyName, String clientName, String localName) {
        String comp = (companyName != null && !companyName.isBlank()) ? companyName : "Pointly";
        String loc = (localName != null && !localName.isBlank()) ? localName : comp;

        return switch (type) {
            case WELCOME_NOTIFICATION -> "¡Bienvenido a " + comp + "!";
            case ALMOST_THERE_NOTIFICATION -> "¡Estás muy cerca de tu beneficio en " + loc + "!";
            case CLIENT_RETENTION_NOTIFICATION -> "¡Te extrañamos en " + comp + "!";
            case POINTS_EXPIRATION_NOTIFICATION -> "¡Tus puntos en " + comp + " están por vencer!";
            case PROMOTION_NOTIFICATION -> "¡Hay promos nuevas esperándote en " + comp + "!";
            case CUSTOM_NOTIFICATION -> "Comunicado de " + comp;
        };
    }

    private String interpolatePlaceholders(String text, Map<String, String> replacements) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String result = text;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}



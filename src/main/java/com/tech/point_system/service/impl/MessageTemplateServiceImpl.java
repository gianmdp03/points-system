package com.tech.point_system.service.impl;

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
import com.tech.point_system.service.CompanyAccessValidator;
import com.tech.point_system.service.MessageTemplateService;
import com.tech.point_system.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private final MessageTemplateRepository messageTemplateRepository;
    private final CompanyRepository companyRepository;
    private final MessageTemplateMapper messageTemplateMapper;
    private final CompanyAccessValidator companyAccessValidator;

    @Override
    @Transactional
    public MessageTemplateDetailDTO addTemplate(String companyAdminId, MessageTemplateRequestDTO dto) {
        Company company = companyAccessValidator.validateAccess(dto.companyId(), companyAdminId);

        MessageTemplate template = messageTemplateMapper.toEntity(dto);
        template.setCompany(company);

        MessageTemplate savedTemplate = messageTemplateRepository.save(template);
        return messageTemplateMapper.toDetailDTO(savedTemplate);
    }

    @Override
    @Transactional
    public MessageTemplateDetailDTO updateTemplate(String companyAdminId, Long companyId, Long id, MessageTemplateUpdateDTO dto) {
        companyAccessValidator.validateAccess(companyId, companyAdminId);

        MessageTemplate template = messageTemplateRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Plantilla de mensaje no encontrada"));

        messageTemplateMapper.updateEntityFromDTO(dto, template);
        MessageTemplate updatedTemplate = messageTemplateRepository.save(template);
        return messageTemplateMapper.toDetailDTO(updatedTemplate);
    }

    @Override
    @Transactional
    public Page<MessageTemplateListDTO> listTemplates(String companyAdminId, Long companyId, Pageable pageable) {
        Company company = companyAccessValidator.validateAccess(companyId, companyAdminId);

        long count = messageTemplateRepository.countByCompanyId(companyId);
        if (count == 0) {
            seedDefaultTemplates(company);
        }

        Page<MessageTemplate> templates = messageTemplateRepository.findByCompanyId(companyId, pageable);
        if (templates.isEmpty()) {
            return Page.empty();
        }
        return templates.map(messageTemplateMapper::toListDTO);
    }

    @Override
    @Transactional
    public List<MessageTemplateDetailDTO> getAllTemplatesByCompany(String companyAdminId, Long companyId) {
        Company company = companyAccessValidator.validateAccess(companyId, companyAdminId);

        long count = messageTemplateRepository.countByCompanyId(companyId);
        if (count == 0) {
            seedDefaultTemplates(company);
        }

        List<MessageTemplate> templates = messageTemplateRepository.findAllByCompanyId(companyId);
        return templates.stream().map(messageTemplateMapper::toDetailDTO).toList();
    }

    @Override
    public MessageTemplateDetailDTO getTemplateById(String companyAdminId, Long companyId, Long id) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);

        MessageTemplate template = messageTemplateRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Plantilla de mensaje no encontrada"));

        return messageTemplateMapper.toDetailDTO(template);
    }

    @Override
    @Transactional
    public void enableOrDisableTemplate(String companyAdminId, Long companyId, Long id) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);

        MessageTemplate template = messageTemplateRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Plantilla de mensaje no encontrada"));

        template.setIsEnabled(!Boolean.TRUE.equals(template.getIsEnabled()));
        messageTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(String companyAdminId, Long companyId, Long id) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);

        MessageTemplate template = messageTemplateRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Plantilla de mensaje no encontrada"));

        messageTemplateRepository.delete(template);
    }

    @Override
    public Optional<MessageTemplate> getRandomActiveTemplate(Long companyId, NotificationType type) {
        List<MessageTemplate> activeTemplates = messageTemplateRepository.findByCompanyIdAndTypeAndIsEnabledTrue(companyId, type);
        if (activeTemplates.isEmpty()) {
            return Optional.empty();
        }
        int index = ThreadLocalRandom.current().nextInt(activeTemplates.size());
        return Optional.of(activeTemplates.get(index));
    }

    @Override
    public MessageTemplateDetailDTO getRandomActiveTemplatePreview(String companyAdminId, Long companyId, NotificationType type) {
        companyAccessValidator.checkAccessOnly(companyId, companyAdminId);

        MessageTemplate template = getRandomActiveTemplate(companyId, type)
                .orElseThrow(() -> new NotFoundException("No hay plantillas activas para el tipo de notificación especificado"));

        return messageTemplateMapper.toDetailDTO(template);
    }

    @Override
    @Transactional
    public void seedDefaultTemplates(Company company) {
        List<MessageTemplate> defaults = createDefaultTemplateEntities(company);
        messageTemplateRepository.saveAll(defaults);
    }

    @Override
    @Transactional
    public void seedDefaultTemplatesForAllCompaniesWithoutTemplates() {
        List<Company> companies = companyRepository.findAll();
        for (Company company : companies) {
            long count = messageTemplateRepository.countByCompanyId(company.getId());
            if (count == 0) {
                seedDefaultTemplates(company);
                log.info("[MESSAGE TEMPLATE] 🌱 6 plantillas por defecto sembradas automáticamente para empresa ID={}: '{}'",
                        company.getId(), company.getName());
            }
        }
    }

    @Override
    @Transactional
    public List<MessageTemplateDetailDTO> resetDefaultTemplates(String companyAdminId, Long companyId) {
        Company company = companyAccessValidator.validateAccess(companyId, companyAdminId);

        List<MessageTemplate> existing = messageTemplateRepository.findByCompanyId(companyId, Pageable.unpaged()).getContent();
        if (!existing.isEmpty()) {
            messageTemplateRepository.deleteAll(existing);
        }

        List<MessageTemplate> defaults = createDefaultTemplateEntities(company);
        List<MessageTemplate> saved = messageTemplateRepository.saveAll(defaults);
        return saved.stream().map(messageTemplateMapper::toDetailDTO).toList();
    }

    private List<MessageTemplate> createDefaultTemplateEntities(Company company) {
        List<MessageTemplate> list = new ArrayList<>();

        // 1. WELCOME_NOTIFICATION
        MessageTemplate welcome = new MessageTemplate();
        welcome.setCompany(company);
        welcome.setName("Bienvenida");
        welcome.setType(NotificationType.WELCOME_NOTIFICATION);
        welcome.setSubject("¡Bienvenido a {empresa}!");
        welcome.setContent("Bienvenido a {empresa}, {nombre}!, muchas gracias por unirte a nosotros!");
        welcome.setIsEnabled(true);
        list.add(welcome);

        // 2. ALMOST_THERE_NOTIFICATION
        MessageTemplate almostThere = new MessageTemplate();
        almostThere.setCompany(company);
        almostThere.setName("Cerca de tu beneficio");
        almostThere.setType(NotificationType.ALMOST_THERE_NOTIFICATION);
        almostThere.setSubject("¡Estás muy cerca de tu beneficio en {local}!");
        almostThere.setContent("¡Estás a solo {puntos_faltantes} puntos de tu beneficio en {local}! Sumá en tu próxima visita y canjealo.");
        almostThere.setIsEnabled(true);
        list.add(almostThere);

        // 3. CLIENT_RETENTION_NOTIFICATION (Opción 1)
        MessageTemplate retention1 = new MessageTemplate();
        retention1.setCompany(company);
        retention1.setName("Retención con saldo de puntos");
        retention1.setType(NotificationType.CLIENT_RETENTION_NOTIFICATION);
        retention1.setSubject("¡Te extrañamos, {nombre}!");
        retention1.setContent("¡Te extrañamos, {nombre}! Tenés {puntos} puntos esperándote. Sumá hoy en nuestros locales de {empresa} y canjea un premio.");
        retention1.setIsEnabled(true);
        list.add(retention1);

        // 4. CLIENT_RETENTION_NOTIFICATION (Opción 2)
        MessageTemplate retention2 = new MessageTemplate();
        retention2.setCompany(company);
        retention2.setName("Retención e invitación a volver");
        retention2.setType(NotificationType.CLIENT_RETENTION_NOTIFICATION);
        retention2.setSubject("Hace bastante no te vemos por {local}");
        retention2.setContent("Hace bastante no te vemos por {local}. Volvé esta semana y mirá las recompensas que tienen para vos.");
        retention2.setIsEnabled(true);
        list.add(retention2);

        // 5. POINTS_EXPIRATION_NOTIFICATION
        MessageTemplate pointsExpiration = new MessageTemplate();
        pointsExpiration.setCompany(company);
        pointsExpiration.setName("Aviso de vencimiento de puntos");
        pointsExpiration.setType(NotificationType.POINTS_EXPIRATION_NOTIFICATION);
        pointsExpiration.setSubject("¡Atención, {nombre}! Tus puntos en {empresa} están por vencer");
        pointsExpiration.setContent("¡Atención, {nombre}! Tus {puntos} puntos en {empresa} vencen en {dias} días. Pasá antes de que expiren y canjealos.");
        pointsExpiration.setIsEnabled(true);
        list.add(pointsExpiration);

        // 6. PROMOTION_NOTIFICATION
        MessageTemplate promotion = new MessageTemplate();
        promotion.setCompany(company);
        promotion.setName("Nuevas promociones");
        promotion.setType(NotificationType.PROMOTION_NOTIFICATION);
        promotion.setSubject("¡Hay promos nuevas esperándote en {empresa}!");
        promotion.setContent("¡Hay promos nuevas esperándote! Entrá a Pointly y descubrí los beneficios de esta semana en {empresa}.");
        promotion.setIsEnabled(true);
        list.add(promotion);

        return list;
    }
}
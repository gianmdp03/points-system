package com.tech.point_system.service;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.dto.messageTemplate.MessageTemplateDetailDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateListDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateRequestDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateUpdateDTO;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.MessageTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MessageTemplateService {
    MessageTemplateDetailDTO addTemplate(String companyAdminId, MessageTemplateRequestDTO dto);
    MessageTemplateDetailDTO updateTemplate(String companyAdminId, Long companyId, Long id, MessageTemplateUpdateDTO dto);
    Page<MessageTemplateListDTO> listTemplates(String companyAdminId, Long companyId, Pageable pageable);
    List<MessageTemplateDetailDTO> getAllTemplatesByCompany(String companyAdminId, Long companyId);
    MessageTemplateDetailDTO getTemplateById(String companyAdminId, Long companyId, Long id);
    void enableOrDisableTemplate(String companyAdminId, Long companyId, Long id);
    void deleteTemplate(String companyAdminId, Long companyId, Long id);
    Optional<MessageTemplate> getRandomActiveTemplate(Long companyId, NotificationType type);
    MessageTemplateDetailDTO getRandomActiveTemplatePreview(String companyAdminId, Long companyId, NotificationType type);
    void seedDefaultTemplates(Company company);
    void seedDefaultTemplatesForAllCompaniesWithoutTemplates();
    List<MessageTemplateDetailDTO> resetDefaultTemplates(String companyAdminId, Long companyId);
}
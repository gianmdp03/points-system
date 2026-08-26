package com.tech.point_system.service;

import com.tech.point_system.dto.messageTemplate.MessageTemplateDetailDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateListDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateRequestDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateUpdateDTO;
import com.tech.point_system.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessageTemplateService {
    MessageTemplateDetailDTO addTemplate(String companyAdminId, MessageTemplateRequestDTO dto);
    MessageTemplateDetailDTO updateTemplate(String companyAdminId, Long companyId, Long id, MessageTemplateUpdateDTO dto);
    Page<MessageTemplateListDTO> listTemplates(String companyAdminId, Long companyId, Pageable pageable);
    MessageTemplateDetailDTO getTemplateById(String companyAdminId, Long companyId, Long id);
    void enableOrDisableTemplate(String companyAdminId, Long companyId, Long id);
    void seedDefaultTemplates(Company company);
    void seedDefaultTemplatesForAllCompaniesWithoutTemplates();
    List<MessageTemplateDetailDTO> resetDefaultTemplates(String companyAdminId, Long companyId);
}
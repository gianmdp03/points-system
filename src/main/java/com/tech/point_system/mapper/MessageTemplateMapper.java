package com.tech.point_system.mapper;

import com.tech.point_system.dto.messageTemplate.MessageTemplateDetailDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateListDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateRequestDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateUpdateDTO;
import com.tech.point_system.model.MessageTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface MessageTemplateMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "isEnabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    MessageTemplate toEntity(MessageTemplateRequestDTO dto);

    MessageTemplateDetailDTO toDetailDTO(MessageTemplate entity);

    MessageTemplateListDTO toListDTO(MessageTemplate entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "isEnabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDTO(MessageTemplateUpdateDTO dto, @MappingTarget MessageTemplate entity);
}
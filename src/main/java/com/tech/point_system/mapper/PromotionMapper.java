package com.tech.point_system.mapper;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.promotion.PromotionRequestDTO;
import com.tech.point_system.dto.promotion.PromotionUpdateDTO;
import com.tech.point_system.model.Promotion;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class PromotionMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    public abstract Promotion toEntity(PromotionRequestDTO dto);
    public abstract PromotionDetailDTO toDetailDTO(Promotion entity);
    public abstract PromotionListDTO toListDTO(Promotion entity);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    public abstract void updateEntityFromDTO(PromotionUpdateDTO dto, @MappingTarget Promotion entity);
}

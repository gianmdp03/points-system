package com.tech.point_system.mapper;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.promotion.PromotionRequestDTO;
import com.tech.point_system.dto.promotion.PromotionUpdateDTO;
import com.tech.point_system.model.Promotion;
import org.mapstruct.*;

@Mapper(
    config = GlobalMapperConfig.class,
    uses = {CompanyMapper.class})
public interface PromotionMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "company", ignore = true)
  Promotion toEntity(PromotionRequestDTO dto);

  PromotionDetailDTO toDetailDTO(Promotion entity);

  PromotionListDTO toListDTO(Promotion entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "company", ignore = true)
  void updateEntityFromDTO(PromotionUpdateDTO dto, @MappingTarget Promotion entity);
}

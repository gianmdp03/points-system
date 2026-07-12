package com.tech.point_system.mapper;

import com.tech.point_system.dto.reward.RewardDetailDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.dto.reward.RewardRequestDTO;
import com.tech.point_system.dto.reward.RewardUpdateDTO;
import com.tech.point_system.model.Reward;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class RewardMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    public abstract Reward toEntity(RewardRequestDTO dto);
    public abstract RewardDetailDTO toDetailDTO(Reward entity);
    public abstract RewardListDTO toListDTO(Reward entity);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    public abstract void updateEntityFromDTO(RewardUpdateDTO dto, @MappingTarget Reward entity);
}

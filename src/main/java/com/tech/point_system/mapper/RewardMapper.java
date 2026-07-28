package com.tech.point_system.mapper;

import com.tech.point_system.dto.reward.RewardDetailDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.dto.reward.RewardRequestDTO;
import com.tech.point_system.dto.reward.RewardUpdateDTO;
import com.tech.point_system.model.Reward;
import org.mapstruct.*;

@Mapper(config = GlobalMapperConfig.class)
public interface RewardMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "isEnabled", ignore = true)
    @Mapping(target = "costInPoints", ignore = true)
    Reward toEntity(RewardRequestDTO dto);
    RewardDetailDTO toDetailDTO(Reward entity);
    RewardListDTO toListDTO(Reward entity);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "isEnabled", ignore = true)
    @Mapping(target = "costInPoints", ignore = true)
    void updateEntityFromDTO(RewardUpdateDTO dto, @MappingTarget Reward entity);
}

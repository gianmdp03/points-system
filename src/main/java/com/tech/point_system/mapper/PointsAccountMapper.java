package com.tech.point_system.mapper;

import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.security.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class, uses = {UserMapper.class})
public interface PointsAccountMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "version", ignore = true)
    PointsAccount toEntity(PointsAccountRequestDTO dto);
    PointsAccountDetailDTO toDetailDTO(PointsAccount entity);
}

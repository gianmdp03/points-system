package com.tech.point_system.mapper;

import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.security.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class, uses = {CompanyMapper.class, UserMapper.class})
public interface PointsAccountMapper {
    @Mapping(target = "id", ignore = true)
    PointsAccount toEntity(PointsAccountRequestDTO dto);
    PointsAccountDetailDTO toDetailDTO(PointsAccount entity);
}

package com.tech.point_system.security.user.mapper;

import com.tech.point_system.mapper.GlobalMapperConfig;
import com.tech.point_system.security.user.dto.user.UserDetailDTO;
import com.tech.point_system.security.user.model.User;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface UserMapper {
    UserDetailDTO toDetailDTO(User entity);
}

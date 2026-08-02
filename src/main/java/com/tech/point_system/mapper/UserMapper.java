package com.tech.point_system.mapper;

import com.tech.point_system.dto.user.UserDetailDTO;
import com.tech.point_system.model.User;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface UserMapper {
    UserDetailDTO toDetailDTO(User entity);
}

package com.tech.point_system.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.NullValuePropertyMappingStrategy;

@MapperConfig(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.FIELD,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GlobalMapperConfig {}

package com.tech.point_system.mapper;

import com.tech.point_system.dto.company.CompanyDetailDTO;
import com.tech.point_system.dto.company.CompanyRequestDTO;
import com.tech.point_system.dto.company.CompanyUpdateDTO;
import com.tech.point_system.model.Company;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class CompanyMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pointsAccounts", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "promotions", ignore = true)
    @Mapping(target = "rewards", ignore = true)
    @Mapping(target = "sales", ignore = true)
    public abstract Company toEntity(CompanyRequestDTO dto);
    public abstract CompanyDetailDTO toDetailDTO(Company entity);
    public abstract Company toListDTO(Company entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pointsAccounts", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "promotions", ignore = true)
    @Mapping(target = "rewards", ignore = true)
    @Mapping(target = "sales", ignore = true)
    public abstract void updateEntityFromDTO(CompanyUpdateDTO dto, @MappingTarget Company entity);
}

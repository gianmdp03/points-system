package com.tech.point_system.mapper;

import com.tech.point_system.dto.company.CompanyDetailDTO;
import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.company.CompanyRequestDTO;
import com.tech.point_system.dto.company.CompanyUpdateDTO;
import com.tech.point_system.model.Company;
import org.mapstruct.*;

@Mapper(config = GlobalMapperConfig.class, uses = {ProductMapper.class, PromotionMapper.class, RewardMapper.class, SaleMapper.class, PointsAccountMapper.class})
public interface CompanyMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pointsAccounts", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "promotions", ignore = true)
    @Mapping(target = "rewards", ignore = true)
    @Mapping(target = "sales", ignore = true)
    @Mapping(target = "admin", ignore = true)
    @Mapping(target = "isEnabled", ignore = true)
    @Mapping(target = "disabledDate", ignore = true)
    @Mapping(target = "appAdminOwner", ignore = true)
    Company toEntity(CompanyRequestDTO dto);
    CompanyDetailDTO toDetailDTO(Company entity);
    CompanyListDTO toListDTO(Company entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pointsAccounts", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "promotions", ignore = true)
    @Mapping(target = "rewards", ignore = true)
    @Mapping(target = "sales", ignore = true)
    @Mapping(target = "admin", ignore = true)
    @Mapping(target = "isEnabled", ignore = true)
    @Mapping(target = "disabledDate", ignore = true)
    @Mapping(target = "appAdminOwner", ignore = true)
    void updateEntityFromDTO(CompanyUpdateDTO dto, @MappingTarget Company entity);
}

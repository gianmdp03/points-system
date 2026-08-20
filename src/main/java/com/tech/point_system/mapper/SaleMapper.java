package com.tech.point_system.mapper;

import com.tech.point_system.dto.sale.SaleDetailDTO;
import com.tech.point_system.dto.sale.SaleListDTO;
import com.tech.point_system.dto.sale.SaleRequestDTO;
import com.tech.point_system.model.Sale;
import org.mapstruct.*;

@Mapper(config = GlobalMapperConfig.class)
public interface SaleMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Sale toEntity(SaleRequestDTO dto);

    SaleDetailDTO toDetailDTO(Sale entity);
    SaleListDTO toListDTO(Sale entity);
}

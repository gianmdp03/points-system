package com.tech.point_system.mapper;

import com.tech.point_system.dto.sale.SaleDetailDTO;
import com.tech.point_system.dto.sale.SaleListDTO;
import com.tech.point_system.dto.sale.SaleRequestDTO;
import com.tech.point_system.model.Sale;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class SaleMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    public abstract Sale toEntity(SaleRequestDTO dto);
    public abstract SaleDetailDTO toDetailDTO(Sale entity);
    public abstract SaleListDTO toListDTO(Sale entity);
}

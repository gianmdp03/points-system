package com.tech.point_system.mapper;

import com.tech.point_system.dto.product.ProductDetailDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.product.ProductRequestDTO;
import com.tech.point_system.dto.product.ProductUpdateDTO;
import com.tech.point_system.model.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class ProductMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    public abstract Product toEntity(ProductRequestDTO dto);
    public abstract ProductDetailDTO toDetailDTO(Product entity);
    public abstract ProductListDTO toListDTO(Product entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    public abstract void updateEntityFromDTO(ProductUpdateDTO dto, @MappingTarget Product entity);
}

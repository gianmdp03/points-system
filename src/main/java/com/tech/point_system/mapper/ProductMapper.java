package com.tech.point_system.mapper;

import com.tech.point_system.dto.product.ProductDetailDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.product.ProductRequestDTO;
import com.tech.point_system.dto.product.ProductUpdateDTO;
import com.tech.point_system.model.Product;
import org.mapstruct.*;

@Mapper(config = GlobalMapperConfig.class, uses = {CompanyMapper.class})
public interface ProductMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    Product toEntity(ProductRequestDTO dto);
    ProductDetailDTO toDetailDTO(Product entity);
    ProductListDTO toListDTO(Product entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    void updateEntityFromDTO(ProductUpdateDTO dto, @MappingTarget Product entity);
}

package com.tech.point_system.service;

import com.tech.point_system.dto.product.ProductDetailDTO;
import com.tech.point_system.dto.product.ProductRequestDTO;
import com.tech.point_system.dto.product.ProductUpdateDTO;

public interface ProductService {
    ProductDetailDTO addProduct(ProductRequestDTO dto);
    ProductDetailDTO updateProduct(Long id, ProductUpdateDTO dto);
    ProductDetailDTO getProduct(Long id);
    void deleteProduct(Long id);
}

package com.tech.point_system.service;

import com.tech.point_system.dto.product.ProductDetailDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.product.ProductRequestDTO;
import com.tech.point_system.dto.product.ProductUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductDetailDTO addProduct(ProductRequestDTO dto);
    Page<ProductListDTO> listProducts(Long companyId, Pageable pageable);
    ProductDetailDTO updateProduct(Long id, ProductUpdateDTO dto);
    ProductDetailDTO getProductById(Long id);
    void deleteProduct(Long id);
}

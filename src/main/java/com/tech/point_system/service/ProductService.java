package com.tech.point_system.service;

import com.tech.point_system.dto.product.ProductDetailDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.product.ProductRequestDTO;
import com.tech.point_system.dto.product.ProductUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductDetailDTO addProduct(String companyAdminId, ProductRequestDTO dto);
    Page<ProductListDTO> listProducts(String companyAdminId, Long companyId, Pageable pageable);
    ProductDetailDTO updateProduct(String companyAdminId, Long companyId, Long id, ProductUpdateDTO dto);
    ProductDetailDTO getProductById(String companyAdminId, Long id);
    void deleteProduct(String companyAdminId, Long companyId, Long id);
}

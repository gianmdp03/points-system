package com.tech.point_system.service.impl;

import com.tech.point_system.dto.company.CompanyListDTO;
import com.tech.point_system.dto.product.ProductDetailDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.product.ProductRequestDTO;
import com.tech.point_system.dto.product.ProductUpdateDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.ProductMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.Product;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.ProductRepository;
import com.tech.point_system.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductDetailDTO addProduct(ProductRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new NotFoundException("El comercio no existe."));

        Product product = productMapper.toEntity(dto);

        product.setCompany(company);

        Product savedProduct = productRepository.save(product);

        return productMapper.toDetailDTO(savedProduct);
    }

    @Override
    public Page<ProductListDTO> listProducts(Long companyId, Pageable pageable) {
        Page<Product> products = productRepository.findByCompanyId(companyId, pageable);

        if(products.isEmpty()) {
            return Page.empty();
        }

        return products.map(productMapper::toListDTO);
    }

    @Override
    public ProductDetailDTO updateProduct(Long id, ProductUpdateDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado."));


        productMapper.updateEntityFromDTO(dto, product);

        Product updatedProduct = productRepository.save(product);

        return productMapper.toDetailDTO(updatedProduct);
    }

    @Override
    public ProductDetailDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado."));

        return productMapper.toDetailDTO(product);
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Producto no encontrado.");
        }
        productRepository.deleteById(id);
    }
}
package com.tech.point_system.service.impl;

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
import com.tech.point_system.security.user.service.CompanyAccessValidator;
import com.tech.point_system.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
  private final CompanyAccessValidator companyAccessValidator;
  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  @Override
  @Transactional
  public ProductDetailDTO addProduct(String companyAdminId, ProductRequestDTO dto) {
    Company company = companyAccessValidator.validateAccess(dto.companyId(), companyAdminId);

    Product product = productMapper.toEntity(dto);

    product.setCompany(company);

    Product savedProduct = productRepository.save(product);

    return productMapper.toDetailDTO(savedProduct);
  }

  @Override
  public Page<ProductListDTO> listProducts(
      String companyAdminId, Long companyId, Pageable pageable) {
    Company company = companyAccessValidator.validateAccess(companyId, companyAdminId);
    Page<Product> products = productRepository.findByCompany(company, pageable);
    if (products.isEmpty()) {
      return Page.empty();
    }

    return products.map(productMapper::toListDTO);
  }

  @Override
  @Transactional
  public ProductDetailDTO updateProduct(String companyAdminId, Long companyId, Long id, ProductUpdateDTO dto) {
    companyAccessValidator.validateAccess(companyId, companyAdminId);
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));

    productMapper.updateEntityFromDTO(dto, product);

    Product updatedProduct = productRepository.save(product);

    return productMapper.toDetailDTO(updatedProduct);
  }

  @Override
  public ProductDetailDTO getProductById(String companyAdminId, Long id) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));

    companyAccessValidator.validateAccess(product.getCompany().getId(), companyAdminId);

    return productMapper.toDetailDTO(product);
  }

  @Override
  @Transactional
  public void deleteProduct(String companyAdminId, Long companyId, Long id) {
    companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
    productRepository.deleteById(id);
  }
}

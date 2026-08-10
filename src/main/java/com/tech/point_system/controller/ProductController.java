package com.tech.point_system.controller;

import com.tech.point_system.dto.product.ProductDetailDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.product.ProductRequestDTO;
import com.tech.point_system.dto.product.ProductUpdateDTO;
import com.tech.point_system.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;


    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping
    public ResponseEntity<ProductDetailDTO> addProduct(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProductRequestDTO dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(jwt.getSubject(), dto));
    }
    
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN' , 'APP_ADMIN' , 'USER')")
    @GetMapping("/{companyId}")
    public ResponseEntity<Page<ProductListDTO>> listProducts(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PageableDefault(page = 0, size = 12) Pageable pageable){
        return ResponseEntity.ok(productService.listProducts(jwt.getSubject(), companyId, pageable));
    }


    @PreAuthorize("hasAnyRole('COMPANY_ADMIN' , 'APP_ADMIN')")
    @PutMapping("/{companyId}/{id}")
    public ResponseEntity<ProductDetailDTO> updateProduct(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PathVariable Long id, @RequestBody ProductUpdateDTO dto){

        return ResponseEntity.ok(productService.updateProduct(jwt.getSubject(), companyId, id, dto));
    }

    @PreAuthorize("hasAnyRole('COMPANY_ADMIN' , 'APP_ADMIN' , 'USER')")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailDTO> getProductById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(jwt.getSubject(), id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_ADMIN')")
    @DeleteMapping("/{companyId}/{id}")
    public ResponseEntity<Void> deleteProduct(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PathVariable Long id) {
        productService.deleteProduct(jwt.getSubject(), companyId, id);
        return ResponseEntity.noContent().build();
    }
}

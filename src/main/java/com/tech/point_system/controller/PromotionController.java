package com.tech.point_system.controller;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.promotion.PromotionRequestDTO;
import com.tech.point_system.dto.promotion.PromotionUpdateDTO;
import com.tech.point_system.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class PromotionController {
    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<PromotionDetailDTO> addPromotion(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PromotionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.addPromotion(jwt.getSubject(), dto));
    }

    @PatchMapping("/{companyId}/{id}")
    public ResponseEntity<PromotionDetailDTO> updatePromotion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PathVariable Long id, @Valid @RequestBody PromotionUpdateDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(promotionService.updatePromotion(jwt.getSubject(), companyId, id, dto));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<Page<PromotionListDTO>> listPromotions(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(promotionService.listPromotions(jwt.getSubject(), companyId, pageable));
    }

    @GetMapping("/{companyId}/{id}")
    public ResponseEntity<PromotionDetailDTO> getPromotionById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(promotionService.getPromotionById(jwt.getSubject(), companyId, id));
    }

    @DeleteMapping("/{companyId}/{id}")
    public ResponseEntity<Void> deletePromotion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PathVariable Long id) {
        promotionService.deletePromotion(jwt.getSubject(), companyId, id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

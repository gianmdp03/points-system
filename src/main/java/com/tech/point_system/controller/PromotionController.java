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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class PromotionController {
    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<PromotionDetailDTO> addPromotion(@Valid @RequestBody PromotionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.addPromotion(dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PromotionDetailDTO> updatePromotion(@PathVariable Long id, @Valid @RequestBody PromotionUpdateDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(promotionService.updatePromotion(id,dto));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<Page<PromotionListDTO>> listPromotions(@PathVariable Long companyId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(promotionService.listPromotions(companyId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionDetailDTO> getPromotionById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(promotionService.getPromotionById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

package com.tech.point_system.controller;

import com.tech.point_system.dto.promotion.PromotionDetailDTO;
import com.tech.point_system.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService promotionService;

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping
    public ResponseEntity<PromotionDetailDTO>
}

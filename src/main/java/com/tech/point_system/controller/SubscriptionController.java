package com.tech.point_system.controller;

import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.dto.subscription.*;
import com.tech.point_system.service.ProrationCalculatorService;
import com.tech.point_system.service.SubscriptionPlanConfigService;
import com.tech.point_system.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionPlanConfigService subscriptionPlanConfigService;
    private final ProrationCalculatorService prorationCalculatorService;

    @GetMapping("/plans")
    public ResponseEntity<List<PlanConfigDTO>> getPlans() {
        log.info("[SUBSCRIPTION REST] 📋 [GET /api/subscriptions/plans] Listando catálogo de planes públicos");
        return ResponseEntity.ok(subscriptionPlanConfigService.getCommercialPlans());
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping
    public ResponseEntity<SubscriptionResponseDTO> createSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubscriptionRequestDTO dto) {
        String userId = jwt.getSubject();
        log.info("[SUBSCRIPTION REST] 🚀 [POST /api/subscriptions] Solicitud de suscripción/extensión para usuario '{}' | Plan='{}' | Periodo='{}' | Provider='{}'",
                userId, dto.plan(), dto.billingPeriod(), dto.provider());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribeCompanyAdmin(userId, dto));
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @GetMapping("/proration-preview")
    public ResponseEntity<ProrationPreviewResponseDTO> getProrationPreview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam SubscriptionPlan newPlan) {
        String userId = jwt.getSubject();
        log.info("[SUBSCRIPTION REST] 🧮 [GET /api/subscriptions/proration-preview] Consultando preview de prorrateo para usuario '{}' -> PlanDestino='{}'",
                userId, newPlan);
        return ResponseEntity.ok(prorationCalculatorService.previewUpgrade(userId, newPlan));
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PatchMapping("/upgrade")
    public ResponseEntity<SubscriptionResponseDTO> upgradeSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) SubscriptionUpgradeRequestDTO dto,
            @RequestParam(name = "newPlan", required = false) SubscriptionPlan queryPlan) {
        String userId = jwt.getSubject();
        if (dto != null && dto.newPlan() != null) {
            log.info("[SUBSCRIPTION REST] ⬆️ [PATCH /api/subscriptions/upgrade] Upgrade para usuario '{}' -> Plan='{}'",
                    userId, dto.newPlan());
            return ResponseEntity.ok(subscriptionService.upgradeSubscription(userId, dto));
        }
        log.info("[SUBSCRIPTION REST] ⬆️ [PATCH /api/subscriptions/upgrade] Upgrade simple para usuario '{}' -> Plan='{}'",
                userId, queryPlan);
        return ResponseEntity.ok(subscriptionService.upgradeSubscription(userId, queryPlan));
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<SubscriptionDetailDTO> getMySubscription(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("[SUBSCRIPTION REST] 🔍 [GET /api/subscriptions/me] Consultando estado de suscripción para usuario '{}'", userId);
        return ResponseEntity.ok(subscriptionService.getMySubscription(userId));
    }
}





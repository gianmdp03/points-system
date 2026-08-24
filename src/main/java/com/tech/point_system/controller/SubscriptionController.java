package com.tech.point_system.controller;

import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.dto.subscription.PlanConfigDTO;
import com.tech.point_system.dto.subscription.SubscriptionDetailDTO;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.service.SubscriptionPlanConfigService;
import com.tech.point_system.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionPlanConfigService subscriptionPlanConfigService;

    @GetMapping("/plans")
    public ResponseEntity<List<PlanConfigDTO>> getPlans() {
        return ResponseEntity.ok(subscriptionPlanConfigService.getCommercialPlans());
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping
    public ResponseEntity<SubscriptionResponseDTO> createSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubscriptionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribeCompanyAdmin(jwt.getSubject(), dto));
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PatchMapping("/change-plan")
    public ResponseEntity<SubscriptionDetailDTO> changeSubscriptionPlan(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam SubscriptionPlan newPlan) {
        return ResponseEntity.ok(subscriptionService.changeSubscriptionPlan(jwt.getSubject(), newPlan));
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PatchMapping("/upgrade")
    public ResponseEntity<SubscriptionDetailDTO> upgradeSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam SubscriptionPlan newPlan) {
        return ResponseEntity.ok(subscriptionService.changeSubscriptionPlan(jwt.getSubject(), newPlan));
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<SubscriptionDetailDTO> getMySubscription(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(jwt.getSubject()));
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @DeleteMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription(@AuthenticationPrincipal Jwt jwt) {
        subscriptionService.cancelSubscription(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}

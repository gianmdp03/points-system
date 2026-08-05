package com.tech.point_system.controller;

import com.tech.point_system.dto.subscription.SubscriptionDetailDTO;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping
    public ResponseEntity<SubscriptionResponseDTO> createSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubscriptionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribeCompanyAdmin(jwt.getSubject(), dto));
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
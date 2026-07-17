package com.tech.point_system.controller;

import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import com.tech.point_system.service.PointsAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points-accounts")
@RequiredArgsConstructor
public class PointsAccountController {
  private final PointsAccountService service;

  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  @PostMapping
  public ResponseEntity<PointsAccountDetailDTO> registerClientAndCreateAccount(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PointsAccountRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.registerClientAndCreateAccount(jwt.getSubject(), dto));
  }
}

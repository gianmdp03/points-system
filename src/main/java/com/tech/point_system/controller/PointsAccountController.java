package com.tech.point_system.controller;

import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import com.tech.point_system.dto.pointsTransaction.PointsTransactionDetailDTO;
import com.tech.point_system.service.PointsAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points-accounts")
@RequiredArgsConstructor
public class PointsAccountController {
  private final PointsAccountService pointsAccountService;

  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  @PostMapping
  public ResponseEntity<PointsAccountDetailDTO> registerClientAndCreateAccount(
          @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PointsAccountRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(pointsAccountService.registerClientAndCreateAccount(jwt.getSubject(), dto));
  }

  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  @GetMapping("/{companyId}")
  public ResponseEntity<Page<PointsAccountDetailDTO>> listPointsAccounts(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(pointsAccountService.listPointsAccounts(jwt.getSubject(), companyId, pageable));
  }

  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  @GetMapping("/{companyId}/inactive")
  public ResponseEntity<Page<PointsAccountDetailDTO>> listInactiveClients(
          @AuthenticationPrincipal Jwt jwt,
          @PathVariable Long companyId,
          @RequestParam(name = "days", defaultValue = "30") Integer days,
          @PageableDefault(size = 10, sort = "lastActivityDate", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
            .body(pointsAccountService.listInactiveClients(jwt.getSubject(), companyId, days, pageable));
  }

  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  @GetMapping("/history/{clientId}/{companyId}")
  public ResponseEntity<Page<PointsTransactionDetailDTO>> getTransactionHistory(@PathVariable Long clientId, @PathVariable Long companyId, @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(pointsAccountService.getTransactionHistory(clientId, companyId, pageable));
  }
}
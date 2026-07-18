package com.tech.point_system.controller;

import com.tech.point_system.dto.pointsAccount.PointsAccountDetailDTO;
import com.tech.point_system.dto.pointsAccount.PointsAccountRequestDTO;
import com.tech.point_system.model.PointsTransaction;
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

  @GetMapping("/{accountId}/transactions")
    public ResponseEntity<Page<PointsTransaction>> getTransactionHistory(@PathVariable Long accountId, @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PointsTransaction> history = pointsAccountService.getTransactionHistory(accountId, pageable);
        return ResponseEntity.ok(history);
    }
}

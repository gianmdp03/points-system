package com.tech.point_system.controller;

import com.tech.point_system.dto.reward.*;
import com.tech.point_system.service.RewardService;
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
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class RewardController {
    private final RewardService rewardService;

    @PostMapping
    public ResponseEntity<RewardDetailDTO> addReward(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RewardRequestDTO dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(rewardService.addReward(jwt.getSubject(), dto));
    }

    @PostMapping("/redeem")
    public ResponseEntity<Void> redeemReward(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RewardRedeemDTO dto) {
        rewardService.redeemReward(jwt.getSubject(), dto.companyId(), dto.rewardId(), dto.userDni());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{companyId}/{id}")
    public ResponseEntity<RewardDetailDTO> updateReward(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PathVariable Long id, RewardUpdateDTO dto){
        return ResponseEntity.ok(rewardService.updateReward(jwt.getSubject(), companyId, id, dto));
    }

    @GetMapping("/{companyId)")
    public ResponseEntity<Page<RewardListDTO>> listRewards(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PageableDefault(page = 0, size = 12)Pageable pageable){
        return ResponseEntity.ok(rewardService.listRewards(jwt.getSubject(), companyId, pageable));
    }

    @GetMapping("/{companyId}/{id}")
    public ResponseEntity<RewardDetailDTO> getRewardById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PathVariable Long id) {
        return ResponseEntity.ok(rewardService.getRewardById(jwt.getSubject(), companyId, id));
    }

    @DeleteMapping("/{companyId}/{id}")
    public ResponseEntity<Void> enableOrDisableReward(@AuthenticationPrincipal Jwt jwt, @PathVariable Long companyId, @PathVariable Long id){
        rewardService.enableOrDisableReward(jwt.getSubject(), companyId, id);
        return ResponseEntity.noContent().build();
    }
}

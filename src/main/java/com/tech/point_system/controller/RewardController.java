package com.tech.point_system.controller;

import com.tech.point_system.dto.product.ProductDetailDTO;
import com.tech.point_system.dto.reward.RewardDetailDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.dto.reward.RewardRequestDTO;
import com.tech.point_system.dto.reward.RewardUpdateDTO;
import com.tech.point_system.service.RewardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class RewardController {
    private final RewardService rewardService;

    @PostMapping
    public ResponseEntity<RewardDetailDTO> addReward(@Valid @RequestBody RewardRequestDTO dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(rewardService.addReward(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RewardDetailDTO> updateReward(@PathVariable Long id, RewardUpdateDTO dto){
        return ResponseEntity.ok(rewardService.updateReward(id, dto));
    }

    @GetMapping("/{companyId)")
    public ResponseEntity<Page<RewardListDTO>> listRewards(@PathVariable Long companyId, @PageableDefault(page = 0, size = 12)Pageable pageable){
        return ResponseEntity.ok(rewardService.listRewards(companyId, pageable));

    }

    @GetMapping("/{id}")
    public ResponseEntity<RewardDetailDTO> getRewardById(@PathVariable Long id) {
        return ResponseEntity.ok(rewardService.getRewardById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReward(@PathVariable Long id){
        rewardService.deleteReward(id);
        return ResponseEntity.noContent().build();
    }
}

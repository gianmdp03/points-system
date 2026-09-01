package com.tech.point_system.controller;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.dto.messageTemplate.MessageTemplateDetailDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateListDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateRequestDTO;
import com.tech.point_system.dto.messageTemplate.MessageTemplateUpdateDTO;
import com.tech.point_system.service.MessageTemplateService;
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

import java.util.List;

@RestController
@RequestMapping("/api/message-templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class MessageTemplateController {

    private final MessageTemplateService messageTemplateService;

    @PostMapping
    public ResponseEntity<MessageTemplateDetailDTO> addTemplate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MessageTemplateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageTemplateService.addTemplate(jwt.getSubject(), dto));
    }

    @PutMapping("/{companyId}/{id}")
    public ResponseEntity<MessageTemplateDetailDTO> updateTemplate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId,
            @PathVariable Long id,
            @Valid @RequestBody MessageTemplateUpdateDTO dto) {
        return ResponseEntity.ok(messageTemplateService.updateTemplate(jwt.getSubject(), companyId, id, dto));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<Page<MessageTemplateListDTO>> listTemplates(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId,
            @PageableDefault(page = 0, size = 12) Pageable pageable) {
        return ResponseEntity.ok(messageTemplateService.listTemplates(jwt.getSubject(), companyId, pageable));
    }

    @GetMapping("/{companyId}/all")
    public ResponseEntity<List<MessageTemplateDetailDTO>> getAllTemplates(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId) {
        return ResponseEntity.ok(messageTemplateService.getAllTemplatesByCompany(jwt.getSubject(), companyId));
    }

    @GetMapping("/{companyId}/random-preview")
    public ResponseEntity<MessageTemplateDetailDTO> getRandomActiveTemplatePreview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId,
            @RequestParam NotificationType type) {
        return ResponseEntity.ok(messageTemplateService.getRandomActiveTemplatePreview(jwt.getSubject(), companyId, type));
    }

    @GetMapping("/{companyId}/{id}")
    public ResponseEntity<MessageTemplateDetailDTO> getTemplateById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId,
            @PathVariable Long id) {
        return ResponseEntity.ok(messageTemplateService.getTemplateById(jwt.getSubject(), companyId, id));
    }

    @DeleteMapping("/{companyId}/{id}")
    public ResponseEntity<Void> enableOrDisableTemplate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId,
            @PathVariable Long id) {
        messageTemplateService.enableOrDisableTemplate(jwt.getSubject(), companyId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{companyId}/{id}/toggle")
    public ResponseEntity<Void> toggleTemplate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId,
            @PathVariable Long id) {
        messageTemplateService.enableOrDisableTemplate(jwt.getSubject(), companyId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{companyId}/{id}/permanent")
    public ResponseEntity<Void> deleteTemplate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId,
            @PathVariable Long id) {
        messageTemplateService.deleteTemplate(jwt.getSubject(), companyId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{companyId}/reset-defaults")
    public ResponseEntity<List<MessageTemplateDetailDTO>> resetDefaultTemplates(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId) {
        return ResponseEntity.ok(messageTemplateService.resetDefaultTemplates(jwt.getSubject(), companyId));
    }
}
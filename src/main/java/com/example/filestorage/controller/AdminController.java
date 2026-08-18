package com.example.filestorage.controller;

import com.example.filestorage.dto.AuditLogResponse;
import com.example.filestorage.dto.PageResponse;
import com.example.filestorage.dto.UserSummaryResponse;
import com.example.filestorage.repository.AuditLogRepository;
import com.example.filestorage.repository.UserRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserSummaryResponse>> listUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {

        Page<com.example.filestorage.entity.User> result = userRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        return ResponseEntity.ok(PageResponse.from(result, UserSummaryResponse::from));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<PageResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(MAX_PAGE_SIZE) int size) {

        Page<com.example.filestorage.entity.AuditLog> result = auditLogRepository
                .findAllByOrderByTimestampDesc(PageRequest.of(page, size));

        return ResponseEntity.ok(PageResponse.from(result, AuditLogResponse::from));
    }

    @GetMapping("/audit-logs/user/{userId}")
    public ResponseEntity<PageResponse<AuditLogResponse>> listAuditLogsForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(MAX_PAGE_SIZE) int size) {

        Page<com.example.filestorage.entity.AuditLog> result = auditLogRepository
                .findAllByUserIdOrderByTimestampDesc(userId, PageRequest.of(page, size));

        return ResponseEntity.ok(PageResponse.from(result, AuditLogResponse::from));
    }
}

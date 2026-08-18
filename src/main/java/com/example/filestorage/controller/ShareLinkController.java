package com.example.filestorage.controller;

import com.example.filestorage.dto.CreateShareLinkRequest;
import com.example.filestorage.dto.ShareLinkResponse;
import com.example.filestorage.entity.ShareLink;
import com.example.filestorage.security.CustomUserDetails;
import com.example.filestorage.service.ShareLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/share-links")
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    private Long getUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }

    @PostMapping
    public ResponseEntity<ShareLinkResponse> create(
            @Valid @RequestBody CreateShareLinkRequest request, Authentication authentication) {

        Long userId = getUserId(authentication);
        ShareLink link = shareLinkService.createShareLink(
                userId, request.fileId(), request.permission(), request.expiresInHours());

        return ResponseEntity.ok(ShareLinkResponse.from(link));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        shareLinkService.revoke(id, userId);
        return ResponseEntity.noContent().build();
    }
}

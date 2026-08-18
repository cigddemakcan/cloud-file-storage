package com.example.filestorage.dto;

import com.example.filestorage.entity.ShareLink;

import java.time.Instant;

public record ShareLinkResponse(
        Long id,
        String token,
        Long fileId,
        ShareLink.Permission permission,
        Instant expiresAt,
        boolean revoked
) {
    public static ShareLinkResponse from(ShareLink link) {
        return new ShareLinkResponse(
                link.getId(),
                link.getToken(),
                link.getFile().getId(),
                link.getPermission(),
                link.getExpiresAt(),
                link.isRevoked()
        );
    }
}

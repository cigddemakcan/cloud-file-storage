package com.example.filestorage.dto;

import com.example.filestorage.entity.User;

import java.time.Instant;

public record UserSummaryResponse(
        Long id,
        String username,
        String email,
        User.Role role,
        long storageQuota,
        long usedStorage,
        Instant createdAt
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStorageQuota(),
                user.getUsedStorage(),
                user.getCreatedAt()
        );
    }
}

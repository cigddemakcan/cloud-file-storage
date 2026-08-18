package com.example.filestorage.service;

import com.example.filestorage.annotation.Auditable;
import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.FileMetadata;
import com.example.filestorage.entity.ShareLink;
import com.example.filestorage.entity.User;
import com.example.filestorage.exception.ShareLinkExpiredException;
import com.example.filestorage.exception.ShareLinkNotFoundException;
import com.example.filestorage.repository.ShareLinkRepository;
import com.example.filestorage.repository.UserRepository;
import com.example.filestorage.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private static final int DEFAULT_EXPIRY_HOURS = 24;

    private final ShareLinkRepository shareLinkRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final StorageService storageService;
    private final AuditLogService auditLogService;

    @Auditable(action = AuditAction.SHARE_CREATE)
    @Transactional
    public ShareLink createShareLink(Long userId, Long fileId, ShareLink.Permission permission, Integer expiresInHours) {

        FileMetadata file = fileService.getOwnedFile(fileId, userId);

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Kullanici bulunamadi: " + userId));

        int hours = expiresInHours != null ? expiresInHours : DEFAULT_EXPIRY_HOURS;

        ShareLink link = new ShareLink();
        link.setFile(file);
        link.setCreatedBy(creator);
        link.setPermission(permission != null ? permission : ShareLink.Permission.DOWNLOAD);
        link.setExpiresAt(Instant.now().plus(hours, ChronoUnit.HOURS));

        return shareLinkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public ShareLink resolveToken(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(ShareLinkNotFoundException::new);


        if (!link.isUsable() || link.getFile().isDeleted()) {
            throw new ShareLinkExpiredException();
        }

        return link;
    }

    public InputStream downloadViaToken(String token) {
        ShareLink link = resolveToken(token);

        if (link.getPermission() != ShareLink.Permission.DOWNLOAD) {
            throw new ShareLinkExpiredException(); // yetkisiz indirme denemesi
        }

        auditLogService.log(null, com.example.filestorage.entity.AuditAction.SHARE_DOWNLOAD,
                link.getFile().getId(), null, true, "token=" + token);

        return storageService.download(link.getFile().getStorageKey());
    }

    @Auditable(action = AuditAction.SHARE_REVOKE)
    @Transactional
    public void revoke(Long shareLinkId, Long userId) {
        ShareLink link = shareLinkRepository.findByIdAndCreatedById(shareLinkId, userId)
                .orElseThrow(ShareLinkNotFoundException::new);
        link.setRevoked(true);
        shareLinkRepository.save(link);
    }
}

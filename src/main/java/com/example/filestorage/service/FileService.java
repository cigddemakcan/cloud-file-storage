package com.example.filestorage.service;

import com.example.filestorage.annotation.Auditable;
import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.FileMetadata;
import com.example.filestorage.entity.Folder;
import com.example.filestorage.entity.User;
import com.example.filestorage.exception.FileNotFoundException;
import com.example.filestorage.exception.FolderNotFoundException;
import com.example.filestorage.exception.QuotaExceededException;
import com.example.filestorage.metrics.FileStorageMetrics;
import com.example.filestorage.repository.FileMetadataRepository;
import com.example.filestorage.repository.FolderRepository;
import com.example.filestorage.repository.ShareLinkRepository;
import com.example.filestorage.repository.UserRepository;
import com.example.filestorage.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final StorageService storageService;
    private final FileStorageMetrics metrics;


    @Auditable(action = AuditAction.UPLOAD)
    @Transactional
    public FileMetadata uploadFile(
            Long userId,
            MultipartFile multipartFile,
            Long parentFolderId
    ) {
        User user = userRepository.findWithLockById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Kullanıcı bulunamadı: " + userId
                        )
                );

        long incomingSize = multipartFile.getSize();
        long remaining =
                user.getStorageQuota() - user.getUsedStorage();

        if (incomingSize > remaining) {
            throw new QuotaExceededException(
                    incomingSize,
                    remaining
            );
        }

        Folder parentFolder = null;

        if (parentFolderId != null) {
            parentFolder = folderRepository
                    .findByIdAndOwnerId(parentFolderId, userId)
                    .orElseThrow(() ->
                            new FolderNotFoundException(parentFolderId)
                    );
        }

        String objectKey =
                "users/" + userId + "/" + UUID.randomUUID();

        try (InputStream inputStream =
                     multipartFile.getInputStream()) {

            storageService.upload(
                    inputStream,
                    incomingSize,
                    multipartFile.getContentType(),
                    objectKey
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Yüklenecek dosya okunamadı",
                    e
            );
        }

        try {
            FileMetadata metadata = new FileMetadata();

            metadata.setOriginalFileName(
                    multipartFile.getOriginalFilename()
            );
            metadata.setContentType(
                    multipartFile.getContentType()
            );
            metadata.setSize(incomingSize);
            metadata.setStorageKey(objectKey);
            metadata.setOwner(user);
            metadata.setParentFolder(parentFolder);
            metadata.setUpdatedAt(Instant.now());

            FileMetadata saved =
                    fileMetadataRepository.save(metadata);

            user.setUsedStorage(
                    user.getUsedStorage() + incomingSize
            );
            userRepository.save(user);

            metrics.incrementUpload();

            return saved;

        } catch (RuntimeException e) {
            log.error(
                    "Metadata kaydı başarısız. "
                            + "MinIO nesnesi geri siliniyor: {}",
                    objectKey,
                    e
            );

            storageService.delete(objectKey);
            metrics.incrementUploadFailed();

            throw e;
        }
    }


    @Transactional(readOnly = true)
    public FileMetadata getOwnedFile(
            Long fileId,
            Long userId
    ) {
        return fileMetadataRepository
                .findByIdAndOwnerIdAndDeletedFalse(
                        fileId,
                        userId
                )
                .orElseThrow(() ->
                        new FileNotFoundException(fileId)
                );
    }

    @Auditable(action = AuditAction.DOWNLOAD)
    public InputStream downloadFile(
            Long fileId,
            Long userId
    ) {
        FileMetadata file =
                getOwnedFile(fileId, userId);

        metrics.incrementDownload();

        return storageService.download(
                file.getStorageKey()
        );
    }


    @Auditable(action = AuditAction.DELETE)
    @Transactional
    public void deleteFile(
            Long fileId,
            Long userId
    ) {
        FileMetadata file =
                getOwnedFile(fileId, userId);


        file.setDeleted(true);
        file.setDeletedAt(Instant.now());


        file.setParentFolder(null);

        fileMetadataRepository.save(file);


        shareLinkRepository
                .findAllByFileId(fileId)
                .forEach(link -> {
                    link.setRevoked(true);
                    shareLinkRepository.save(link);
                });


        User user = userRepository
                .findWithLockById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Kullanıcı bulunamadı: " + userId
                        )
                );

        user.setUsedStorage(
                Math.max(
                        0,
                        user.getUsedStorage() - file.getSize()
                )
        );

        userRepository.save(user);
    }


    public String generatePresignedDownloadUrl(Long fileId, Long userId, int expirySeconds) {
        FileMetadata file = getOwnedFile(fileId, userId);
        return storageService.generatePresignedDownloadUrl(file.getStorageKey(), expirySeconds);
    }


    @Auditable(action = AuditAction.RESTORE)
    @Transactional
    public FileMetadata restoreFile(Long fileId, Long userId) {
        FileMetadata file = fileMetadataRepository
                .findByIdAndOwnerIdAndDeletedTrue(fileId, userId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        User user = userRepository.findWithLockById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Kullanıcı bulunamadı: " + userId));

        long remaining = user.getStorageQuota() - user.getUsedStorage();
        if (file.getSize() > remaining) {
            throw new QuotaExceededException(file.getSize(), remaining);
        }

        file.setDeleted(false);
        file.setDeletedAt(null);

        fileMetadataRepository.save(file);

        user.setUsedStorage(user.getUsedStorage() + file.getSize());
        userRepository.save(user);

        metrics.incrementRestore();

        return file;
    }


    @Transactional(readOnly = true)
    public List<FileMetadata> listTrash(Long userId) {
        return fileMetadataRepository
                .findAllByOwnerIdAndDeletedTrueOrderByDeletedAtDesc(userId);
    }
}
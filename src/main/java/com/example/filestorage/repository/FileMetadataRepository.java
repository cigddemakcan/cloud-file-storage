package com.example.filestorage.repository;

import com.example.filestorage.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository
        extends JpaRepository<FileMetadata, Long> {


    Optional<FileMetadata> findByIdAndOwnerIdAndDeletedFalse(
            Long id,
            Long ownerId
    );

    List<FileMetadata>
    findAllByParentFolderIdAndOwnerIdAndDeletedFalse(
            Long parentFolderId,
            Long ownerId
    );


    List<FileMetadata>
    findAllByOwnerIdAndParentFolderIsNullAndDeletedFalse(
            Long ownerId
    );


    boolean existsByParentFolderIdAndOwnerIdAndDeletedFalse(
            Long parentFolderId,
            Long ownerId
    );


    List<FileMetadata> findAllByOwnerIdAndDeletedTrueOrderByDeletedAtDesc(
            Long ownerId
    );


    Optional<FileMetadata> findByIdAndOwnerIdAndDeletedTrue(
            Long id,
            Long ownerId
    );


    List<FileMetadata> findAllByDeletedTrueAndDeletedAtBefore(
            Instant cutoff
    );


    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE FileMetadata file
            SET file.parentFolder = NULL
            WHERE file.parentFolder.id = :folderId
              AND file.owner.id = :ownerId
              AND file.deleted = true
            """)
    int detachDeletedFilesFromFolder(
            @Param("folderId") Long folderId,
            @Param("ownerId") Long ownerId
    );
}
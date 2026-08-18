package com.example.filestorage.repository;

import com.example.filestorage.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    Optional<Folder> findByIdAndOwnerId(Long id, Long ownerId);

    List<Folder> findAllByParentFolderIdAndOwnerId(Long parentFolderId, Long ownerId);

    boolean existsByNameAndParentFolderIdAndOwnerId(String name, Long parentFolderId, Long ownerId);
}

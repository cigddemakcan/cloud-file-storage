package com.example.filestorage.repository;

import com.example.filestorage.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByToken(String token);

    Optional<ShareLink> findByIdAndCreatedById(Long id, Long createdById);

    List<ShareLink> findAllByFileId(Long fileId);

    List<ShareLink> findAllByCreatedById(Long createdById);
}

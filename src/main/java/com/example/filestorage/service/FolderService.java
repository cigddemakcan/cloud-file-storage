package com.example.filestorage.service;

import com.example.filestorage.annotation.Auditable;
import com.example.filestorage.dto.FileMetadataResponse;
import com.example.filestorage.dto.FolderContentsResponse;
import com.example.filestorage.dto.FolderResponse;
import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.FileMetadata;
import com.example.filestorage.entity.Folder;
import com.example.filestorage.entity.User;
import com.example.filestorage.exception.DuplicateFolderNameException;
import com.example.filestorage.exception.FolderNotEmptyException;
import com.example.filestorage.exception.FolderNotFoundException;
import com.example.filestorage.repository.FileMetadataRepository;
import com.example.filestorage.repository.FolderRepository;
import com.example.filestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;

    @Transactional
    public Folder createFolder(
            Long userId,
            String name,
            Long parentFolderId
    ) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Kullanıcı bulunamadı: " + userId
                        )
                );

        Folder parentFolder = null;

        if (parentFolderId != null) {

            parentFolder = getOwnedFolder(
                    parentFolderId,
                    userId
            );
        }

        boolean duplicate =
                folderRepository
                        .existsByNameAndParentFolderIdAndOwnerId(
                                name,
                                parentFolderId,
                                userId
                        );

        if (duplicate) {
            throw new DuplicateFolderNameException(name);
        }

        Folder folder = new Folder();
        folder.setName(name);
        folder.setOwner(owner);
        folder.setParentFolder(parentFolder);

        return folderRepository.save(folder);
    }

    @Transactional(readOnly = true)
    public Folder getOwnedFolder(
            Long folderId,
            Long userId
    ) {
        return folderRepository
                .findByIdAndOwnerId(folderId, userId)
                .orElseThrow(() ->
                        new FolderNotFoundException(folderId)
                );
    }


    @Transactional(readOnly = true)
    public FolderContentsResponse getContents(
            Long parentFolderId,
            Long userId
    ) {
        List<Folder> subFolders;
        List<FileMetadata> files;

        if (parentFolderId == null) {
            subFolders =
                    folderRepository
                            .findAllByParentFolderIdAndOwnerId(
                                    null,
                                    userId
                            );

            files =
                    fileMetadataRepository
                            .findAllByOwnerIdAndParentFolderIsNullAndDeletedFalse(
                                    userId
                            );
        } else {

            getOwnedFolder(parentFolderId, userId);

            subFolders =
                    folderRepository
                            .findAllByParentFolderIdAndOwnerId(
                                    parentFolderId,
                                    userId
                            );

            files =
                    fileMetadataRepository
                            .findAllByParentFolderIdAndOwnerIdAndDeletedFalse(
                                    parentFolderId,
                                    userId
                            );
        }

        return new FolderContentsResponse(
                subFolders.stream()
                        .map(FolderResponse::from)
                        .toList(),

                files.stream()
                        .map(FileMetadataResponse::from)
                        .toList()
        );
    }

    @Auditable(action = AuditAction.DELETE)
    @Transactional
    public void deleteFolder(
            Long folderId,
            Long userId
    ) {
        Folder folder = getOwnedFolder(
                folderId,
                userId
        );

        boolean hasSubFolders =
                !folderRepository
                        .findAllByParentFolderIdAndOwnerId(
                                folderId,
                                userId
                        )
                        .isEmpty();

        boolean hasActiveFiles =
                fileMetadataRepository
                        .existsByParentFolderIdAndOwnerIdAndDeletedFalse(
                                folderId,
                                userId
                        );

        if (hasSubFolders || hasActiveFiles) {
            throw new FolderNotEmptyException(folderId);
        }


        fileMetadataRepository.detachDeletedFilesFromFolder(
                folderId,
                userId
        );

        folderRepository.delete(folder);
    }
}
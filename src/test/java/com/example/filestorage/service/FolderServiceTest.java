package com.example.filestorage.service;

import com.example.filestorage.entity.Folder;
import com.example.filestorage.entity.User;
import com.example.filestorage.exception.DuplicateFolderNameException;
import com.example.filestorage.exception.FolderNotEmptyException;
import com.example.filestorage.exception.FolderNotFoundException;
import com.example.filestorage.repository.FileMetadataRepository;
import com.example.filestorage.repository.FolderRepository;
import com.example.filestorage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FolderService - unit testler (Mockito)")
class FolderServiceTest {

    @Mock
    private FolderRepository folderRepository;
    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private UserRepository userRepository;

    private FolderService folderService;

    private User owner;

    @BeforeEach
    void setUp() {
        folderService = new FolderService(folderRepository, fileMetadataRepository, userRepository);

        owner = new User();
        owner.setId(1L);
        owner.setUsername("cigdem");
    }

    @Test
    @DisplayName("Ayni parent altinda ayni isimde klasor varsa DuplicateFolderNameException firlar")
    void createFolder_withDuplicateName_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(folderRepository.existsByNameAndParentFolderIdAndOwnerId("Belgeler", null, 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> folderService.createFolder(1L, "Belgeler", null))
                .isInstanceOf(DuplicateFolderNameException.class);

        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Parent klasor baska bir kullaniciya aitse FolderNotFoundException firlar (IDOR korumasi)")
    void createFolder_withParentOwnedByAnotherUser_throwsFolderNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        when(folderRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.createFolder(1L, "AltKlasor", 99L))
                .isInstanceOf(FolderNotFoundException.class);

        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Klasorde alt klasor varsa silme FolderNotEmptyException ile reddedilir")
    void deleteFolder_withSubFolders_throwsFolderNotEmptyException() {
        Folder folder = new Folder();
        folder.setId(10L);
        folder.setOwner(owner);

        when(folderRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(folder));
        when(folderRepository.findAllByParentFolderIdAndOwnerId(10L, 1L))
                .thenReturn(List.of(new Folder())); // en az bir alt klasor var

        assertThatThrownBy(() -> folderService.deleteFolder(10L, 1L))
                .isInstanceOf(FolderNotEmptyException.class);

        verify(folderRepository, never()).delete(any(Folder.class));
    }

    @Test
    @DisplayName("Klasorde aktif (silinmemis) dosya varsa silme reddedilir")
    void deleteFolder_withActiveFiles_throwsFolderNotEmptyException() {
        Folder folder = new Folder();
        folder.setId(10L);
        folder.setOwner(owner);

        when(folderRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(folder));
        when(folderRepository.findAllByParentFolderIdAndOwnerId(10L, 1L)).thenReturn(List.of());
        when(fileMetadataRepository.existsByParentFolderIdAndOwnerIdAndDeletedFalse(10L, 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> folderService.deleteFolder(10L, 1L))
                .isInstanceOf(FolderNotEmptyException.class);

        verify(folderRepository, never()).delete(any(Folder.class));
    }

    @Test
    @DisplayName("Bos klasor (alt klasor ve aktif dosya yok) basariyla silinir")
    void deleteFolder_whenEmpty_deletesSuccessfully() {
        Folder folder = new Folder();
        folder.setId(10L);
        folder.setOwner(owner);

        when(folderRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(folder));
        when(folderRepository.findAllByParentFolderIdAndOwnerId(10L, 1L)).thenReturn(List.of());
        when(fileMetadataRepository.existsByParentFolderIdAndOwnerIdAndDeletedFalse(10L, 1L))
                .thenReturn(false);

        folderService.deleteFolder(10L, 1L);


        verify(fileMetadataRepository, times(1)).detachDeletedFilesFromFolder(10L, 1L);
        verify(folderRepository, times(1)).delete(folder);
    }
}

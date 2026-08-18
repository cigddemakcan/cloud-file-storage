package com.example.filestorage.service;

import com.example.filestorage.entity.FileMetadata;
import com.example.filestorage.entity.User;
import com.example.filestorage.exception.FileNotFoundException;
import com.example.filestorage.exception.QuotaExceededException;
import com.example.filestorage.metrics.FileStorageMetrics;
import com.example.filestorage.repository.FileMetadataRepository;
import com.example.filestorage.repository.FolderRepository;
import com.example.filestorage.repository.ShareLinkRepository;
import com.example.filestorage.repository.UserRepository;
import com.example.filestorage.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService - unit testler (Mockito)")
class FileServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private ShareLinkRepository shareLinkRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private FileStorageMetrics metrics;

    private FileService fileService;

    private User owner;

    @BeforeEach
    void setUp() {
        fileService = new FileService(
                fileMetadataRepository,
                userRepository,
                folderRepository,
                shareLinkRepository,
                storageService,
                metrics
        );

        owner = new User();
        owner.setId(1L);
        owner.setUsername("cigdem");
        owner.setStorageQuota(1_000_000L);
        owner.setUsedStorage(0L);
    }

    @Test
    @DisplayName("Kota asan yukleme QuotaExceededException firlatir, MinIO'ya hic yazilmaz")
    void uploadFile_whenQuotaExceeded_throwsAndNeverTouchesStorage() {
        owner.setStorageQuota(10L);
        when(userRepository.findWithLockById(1L)).thenReturn(Optional.of(owner));

        MockMultipartFile file = new MockMultipartFile(
                "file", "buyuk.txt", "text/plain",
                "bu icerik kesinlikle 10 byte'tan uzun".getBytes());

        assertThatThrownBy(() -> fileService.uploadFile(1L, file, null))
                .isInstanceOf(QuotaExceededException.class);

        verify(storageService, never()).upload(any(), anyLong(), anyString(), anyString());
        verify(fileMetadataRepository, never()).save(any());
        verify(metrics, never()).incrementUpload();
    }

    @Test
    @DisplayName("Basarili yukleme: MinIO'ya yazilir, metadata kaydedilir, kota guncellenir, metrik artar")
    void uploadFile_whenSuccessful_savesMetadataAndIncrementsMetric() {
        when(userRepository.findWithLockById(1L)).thenReturn(Optional.of(owner));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "merhaba".getBytes());

        FileMetadata savedEntity = new FileMetadata();
        savedEntity.setId(42L);
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(savedEntity);

        FileMetadata result = fileService.uploadFile(1L, file, null);

        assertThat(result.getId()).isEqualTo(42L);
        verify(storageService, times(1)).upload(any(InputStream.class), eq((long) file.getBytes().length),
                eq("text/plain"), anyString());
        verify(metrics, times(1)).incrementUpload();
        verify(metrics, never()).incrementUploadFailed();


        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsedStorage()).isEqualTo(file.getBytes().length);
    }

    @Test
    @DisplayName("Metadata kaydi basarisiz olursa MinIO'daki nesne geri silinir ve hata metrik'i artar")
    void uploadFile_whenMetadataSaveFails_rollsBackStorageObject() {
        when(userRepository.findWithLockById(1L)).thenReturn(Optional.of(owner));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "merhaba".getBytes());

        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenThrow(new RuntimeException("DB baglantisi koptu"));

        assertThatThrownBy(() -> fileService.uploadFile(1L, file, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB baglantisi koptu");


        verify(storageService, times(1)).upload(any(), anyLong(), anyString(), anyString());

        verify(storageService, times(1)).delete(anyString());
        verify(metrics, times(1)).incrementUploadFailed();
        verify(metrics, never()).incrementUpload();
    }

    @Test
    @DisplayName("Var olmayan/baskasina ait dosya istenince FileNotFoundException firlar")
    void getOwnedFile_whenNotFound_throwsFileNotFoundException() {
        when(fileMetadataRepository.findByIdAndOwnerIdAndDeletedFalse(99L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getOwnedFile(99L, 1L))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("Dosya silme MinIO'ya HIC dokunmaz - sadece soft-delete isaretler (Faz 4 tasarim karari)")
    void deleteFile_marksSoftDeleteWithoutTouchingStorage() {
        FileMetadata file = new FileMetadata();
        file.setId(5L);
        file.setSize(100L);
        file.setStorageKey("users/1/abc");

        when(fileMetadataRepository.findByIdAndOwnerIdAndDeletedFalse(5L, 1L))
                .thenReturn(Optional.of(file));
        when(shareLinkRepository.findAllByFileId(5L)).thenReturn(List.of());
        owner.setUsedStorage(100L);
        when(userRepository.findWithLockById(1L)).thenReturn(Optional.of(owner));

        fileService.deleteFile(5L, 1L);

        assertThat(file.isDeleted()).isTrue();
        assertThat(file.getDeletedAt()).isNotNull();
        assertThat(file.getParentFolder()).isNull();

        verify(storageService, never()).delete(anyString());
    }

    @Test
    @DisplayName("Trash'teki dosya kota yetersizse restore edilemez")
    void restoreFile_whenQuotaExceeded_throwsException() {
        FileMetadata file = new FileMetadata();
        file.setId(5L);
        file.setSize(500L);

        when(fileMetadataRepository.findByIdAndOwnerIdAndDeletedTrue(5L, 1L))
                .thenReturn(Optional.of(file));

        owner.setStorageQuota(100L);
        owner.setUsedStorage(0L);
        when(userRepository.findWithLockById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> fileService.restoreFile(5L, 1L))
                .isInstanceOf(QuotaExceededException.class);

        verify(fileMetadataRepository, never()).save(any());
        verify(metrics, never()).incrementRestore();
    }

    @Test
    @DisplayName("Yeterli kota varsa restore basarili olur ve metrik artar")
    void restoreFile_whenSuccessful_incrementsRestoreMetric() {
        FileMetadata file = new FileMetadata();
        file.setId(5L);
        file.setSize(100L);
        file.setDeleted(true);

        when(fileMetadataRepository.findByIdAndOwnerIdAndDeletedTrue(5L, 1L))
                .thenReturn(Optional.of(file));
        when(userRepository.findWithLockById(1L)).thenReturn(Optional.of(owner));

        FileMetadata result = fileService.restoreFile(5L, 1L);

        assertThat(result.isDeleted()).isFalse();
        assertThat(result.getDeletedAt()).isNull();
        verify(metrics, times(1)).incrementRestore();
    }
}

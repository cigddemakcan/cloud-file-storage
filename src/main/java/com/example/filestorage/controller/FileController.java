package com.example.filestorage.controller;

import com.example.filestorage.dto.FileMetadataResponse;
import com.example.filestorage.entity.FileMetadata;
import com.example.filestorage.security.CustomUserDetails;
import com.example.filestorage.service.FileService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;

    private Long getUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetadataResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentFolderId", required = false)
            @Positive(message = "parentFolderId pozitif bir sayi olmali") Long parentFolderId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        FileMetadata saved = fileService.uploadFile(userId, file, parentFolderId);
        return ResponseEntity.ok(FileMetadataResponse.from(saved));
    }

    @GetMapping("/{id}/metadata")
    public ResponseEntity<FileMetadataResponse> getMetadata(
            @PathVariable Long id, Authentication authentication) {

        Long userId = getUserId(authentication);
        FileMetadata file = fileService.getOwnedFile(id, userId);
        return ResponseEntity.ok(FileMetadataResponse.from(file));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable Long id, Authentication authentication) {

        Long userId = getUserId(authentication);
        FileMetadata file = fileService.getOwnedFile(id, userId);

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = fileService.downloadFile(id, userId)) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalFileName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.getSize()))
                .body(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        fileService.deleteFile(id, userId);
        return ResponseEntity.noContent().build();
    }

    // --- Trash / Cop Kutusu ---

    @GetMapping("/trash")
    public ResponseEntity<List<FileMetadataResponse>> listTrash(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<FileMetadataResponse> trash = fileService.listTrash(userId).stream()
                .map(FileMetadataResponse::from)
                .toList();
        return ResponseEntity.ok(trash);
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<FileMetadataResponse> restore(
            @PathVariable Long id, Authentication authentication) {

        Long userId = getUserId(authentication);
        FileMetadata restored = fileService.restoreFile(id, userId);
        return ResponseEntity.ok(FileMetadataResponse.from(restored));
    }

    // --- Presigned URL ---

    @GetMapping("/{id}/presigned-url")
    public ResponseEntity<Map<String, Object>> presignedUrl(
            @PathVariable Long id,
            @RequestParam(defaultValue = "900") int expiresInSeconds,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String url = fileService.generatePresignedDownloadUrl(id, userId, expiresInSeconds);

        return ResponseEntity.ok(Map.of(
                "url", url,
                "expiresInSeconds", expiresInSeconds
        ));
    }
}

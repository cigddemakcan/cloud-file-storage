package com.example.filestorage.controller;

import com.example.filestorage.dto.CreateFolderRequest;
import com.example.filestorage.dto.FolderContentsResponse;
import com.example.filestorage.dto.FolderResponse;
import com.example.filestorage.entity.Folder;
import com.example.filestorage.security.CustomUserDetails;
import com.example.filestorage.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    private Long getUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }

    @PostMapping
    public ResponseEntity<FolderResponse> create(
            @Valid @RequestBody CreateFolderRequest request, Authentication authentication) {

        Long userId = getUserId(authentication);
        Folder folder = folderService.createFolder(userId, request.name(), request.parentFolderId());
        return ResponseEntity.ok(FolderResponse.from(folder));
    }


    @GetMapping("/contents")
    public ResponseEntity<FolderContentsResponse> getRootContents(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(folderService.getContents(null, userId));
    }

    @GetMapping("/{id}/contents")
    public ResponseEntity<FolderContentsResponse> getContents(
            @PathVariable Long id, Authentication authentication) {

        Long userId = getUserId(authentication);
        return ResponseEntity.ok(folderService.getContents(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        folderService.deleteFolder(id, userId);
        return ResponseEntity.noContent().build();
    }
}

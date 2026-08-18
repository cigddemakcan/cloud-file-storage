package com.example.filestorage.controller;

import com.example.filestorage.entity.FileMetadata;
import com.example.filestorage.entity.ShareLink;
import com.example.filestorage.service.ShareLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareLinkService shareLinkService;

    @GetMapping("/{token}/metadata")
    public ResponseEntity<Map<String, Object>> metadata(@PathVariable String token) {
        ShareLink link = shareLinkService.resolveToken(token);
        FileMetadata file = link.getFile();

        return ResponseEntity.ok(Map.of(
                "fileName", file.getOriginalFileName(),
                "contentType", file.getContentType(),
                "size", file.getSize(),
                "permission", link.getPermission().name(),
                "expiresAt", link.getExpiresAt().toString()
        ));
    }

    @GetMapping("/{token}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String token) {
        ShareLink link = shareLinkService.resolveToken(token);
        FileMetadata file = link.getFile();

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = shareLinkService.downloadViaToken(token)) {
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
}

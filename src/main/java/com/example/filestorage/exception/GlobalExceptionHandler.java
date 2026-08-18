package com.example.filestorage.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFileNotFound(FileNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(FolderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFolderNotFound(FolderNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ShareLinkNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleShareLinkNotFound(ShareLinkNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ShareLinkExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleShareLinkExpired(ShareLinkExpiredException e) {
        return buildResponse(HttpStatus.GONE, e.getMessage());
    }

    @ExceptionHandler(FolderNotEmptyException.class)
    public ResponseEntity<Map<String, Object>> handleFolderNotEmpty(FolderNotEmptyException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DuplicateFolderNameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateFolderName(DuplicateFolderNameException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceeded(QuotaExceededException e) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(RefreshTokenReuseDetectedException.class)
    public ResponseEntity<Map<String, Object>> handleRefreshTokenReuse(RefreshTokenReuseDetectedException e) {

        return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyAttempts(TooManyAttemptsException e) {

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                        "message", e.getMessage(),
                        "retryAfterSeconds", e.getRetryAfterSeconds()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", "Gecersiz istek verisi",
                "errors", fieldErrors
        ));
    }


    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException e) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();

            String fieldName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.put(fieldName, violation.getMessage());
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", "Gecersiz istek verisi",
                "errors", fieldErrors
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "Dosya boyutu izin verilen maksimum boyutu asiyor");
    }

    @ExceptionHandler(com.example.filestorage.storage.StorageException.class)
    public ResponseEntity<Map<String, Object>> handleStorageException(
            com.example.filestorage.storage.StorageException e) {

        log.error("Storage katmani hatasi", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Depolama islemi sirasinda bir hata olustu");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Beklenmeyen hata", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata olustu");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "message", message
        ));
    }
}

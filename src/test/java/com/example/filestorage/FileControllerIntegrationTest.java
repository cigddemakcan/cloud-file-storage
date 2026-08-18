package com.example.filestorage;

import com.example.filestorage.dto.FileMetadataResponse;
import com.example.filestorage.entity.User;
import com.example.filestorage.repository.UserRepository;
import com.example.filestorage.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("File - upload/download/delete/restore akisi (gercek PostgreSQL + MinIO container ile)")
class FileControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private HttpEntity<MultiValueMap<String, Object>> buildUploadRequest(
            String token, String fileName, byte[] content) {

        ByteArrayResource fileResource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = AuthTestSupport.bearerHeaders(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(body, headers);
    }

    @Test
    @DisplayName("Dosya yuklenip indirilebiliyor ve icerik degismeden geri donuyor")
    void uploadThenDownload_returnsIdenticalContent() {
        String token = AuthTestSupport.registerAndLogin(restTemplate);
        byte[] originalContent = "merhaba dunya, bu bir test dosyasi".getBytes();

        ResponseEntity<FileMetadataResponse> uploadResponse = restTemplate.postForEntity(
                "/api/files/upload",
                buildUploadRequest(token, "test.txt", originalContent),
                FileMetadataResponse.class);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long fileId = uploadResponse.getBody().id();
        assertThat(fileId).isNotNull();

        HttpEntity<Void> authOnly = new HttpEntity<>(AuthTestSupport.bearerHeaders(token));
        ResponseEntity<byte[]> downloadResponse = restTemplate.exchange(
                "/api/files/" + fileId + "/download",
                HttpMethod.GET, authOnly, byte[].class);

        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResponse.getBody()).isEqualTo(originalContent);
    }

    @Test
    @DisplayName("Bir kullanicinin dosyasina baska kullanici ID degistirerek erisemez (IDOR koruması)")
    void download_asAnotherUser_returns404NotLeaksData() {
        String ownerToken = AuthTestSupport.registerAndLogin(restTemplate);
        ResponseEntity<FileMetadataResponse> uploadResponse = restTemplate.postForEntity(
                "/api/files/upload",
                buildUploadRequest(ownerToken, "gizli.txt", "gizli icerik".getBytes()),
                FileMetadataResponse.class);
        Long fileId = uploadResponse.getBody().id();


        String attackerToken = AuthTestSupport.registerAndLogin(restTemplate);
        HttpEntity<Void> attackerAuth = new HttpEntity<>(AuthTestSupport.bearerHeaders(attackerToken));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/files/" + fileId + "/download",
                HttpMethod.GET, attackerAuth, String.class);


        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Dosya silinince trash'e duser, restore ile geri gelir")
    void deleteThenRestore_bringsFileBack() {
        String token = AuthTestSupport.registerAndLogin(restTemplate);
        ResponseEntity<FileMetadataResponse> uploadResponse = restTemplate.postForEntity(
                "/api/files/upload",
                buildUploadRequest(token, "silinecek.txt", "icerik".getBytes()),
                FileMetadataResponse.class);
        Long fileId = uploadResponse.getBody().id();

        HttpEntity<Void> auth = new HttpEntity<>(AuthTestSupport.bearerHeaders(token));

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/files/" + fileId, HttpMethod.DELETE, auth, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> metadataAfterDelete = restTemplate.exchange(
                "/api/files/" + fileId + "/metadata", HttpMethod.GET, auth, String.class);
        assertThat(metadataAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);


        ResponseEntity<FileMetadataResponse[]> trashResponse = restTemplate.exchange(
                "/api/files/trash", HttpMethod.GET, auth, FileMetadataResponse[].class);
        assertThat(trashResponse.getBody())
                .extracting(FileMetadataResponse::id)
                .contains(fileId);


        ResponseEntity<FileMetadataResponse> restoreResponse = restTemplate.exchange(
                "/api/files/" + fileId + "/restore", HttpMethod.POST, auth, FileMetadataResponse.class);
        assertThat(restoreResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<FileMetadataResponse> metadataAfterRestore = restTemplate.exchange(
                "/api/files/" + fileId + "/metadata", HttpMethod.GET, auth, FileMetadataResponse.class);
        assertThat(metadataAfterRestore.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Normal boyutta dosya yuklemesi kota kontrolunu basariyla gecer")
    void upload_withinQuota_succeeds() {
        String token = AuthTestSupport.registerAndLogin(restTemplate);

        ResponseEntity<FileMetadataResponse> response = restTemplate.postForEntity(
                "/api/files/upload",
                buildUploadRequest(token, "kucuk.txt", "kucuk icerik".getBytes()),
                FileMetadataResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Kota asan dosya yuklemesi 413 ile reddedilir")
    void upload_exceedingQuota_returns413() {
        String token = AuthTestSupport.registerAndLogin(restTemplate);

        Long userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId).orElseThrow();
        user.setStorageQuota(5);
        userRepository.save(user);

        byte[] content = "bu icerik kesinlikle 5 byte'tan uzun".getBytes();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/files/upload",
                buildUploadRequest(token, "buyuk.txt", content),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }
}

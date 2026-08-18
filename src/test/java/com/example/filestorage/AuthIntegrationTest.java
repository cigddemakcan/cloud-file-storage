package com.example.filestorage;

import com.example.filestorage.dto.AuthResponse;
import com.example.filestorage.dto.LoginRequest;
import com.example.filestorage.dto.RefreshRequest;
import com.example.filestorage.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Auth - register & login akisi (gercek PostgreSQL container ile)")
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Gecerli bilgilerle register olunca 200 ve token doner")
    void register_withValidData_returnsToken() {
        String username = "yeniuser_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = new RegisterRequest(
                username, username + "@example.com", "Sifre123!");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    @DisplayName("Ayni username ile iki kere register olunca 400 doner")
    void register_withDuplicateUsername_returnsBadRequest() {
        String username = "tekrareden_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = new RegisterRequest(
                username, username + "@example.com", "Sifre123!");

        restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);


        RegisterRequest duplicate = new RegisterRequest(
                username, "farkli_" + username + "@example.com", "Sifre123!");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", duplicate, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Dogru sifre ile login olunca token doner")
    void login_withCorrectPassword_returnsToken() {
        String username = "loginuser_" + UUID.randomUUID().toString().substring(0, 8);
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest(username, username + "@example.com", "Sifre123!"),
                AuthResponse.class);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(username, "Sifre123!"),
                AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    @DisplayName("Yanlis sifre ile login denemesi basarisiz olur")
    void login_withWrongPassword_fails() {
        String username = "yanlissifre_" + UUID.randomUUID().toString().substring(0, 8);
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest(username, username + "@example.com", "DogruSifre123!"),
                AuthResponse.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(username, "YanlisSifre999!"),
                String.class);


        assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Korumali bir endpoint'e token olmadan erisim 401 doner")
    void protectedEndpoint_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/folders/contents", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Gecersiz email formati ile register 400 doner ve alan hatasi icerir")
    void register_withInvalidEmail_returns400WithFieldError() {
        String username = "gecersizemail_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = new RegisterRequest(username, "gecerli-olmayan-email", "Sifre123!");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("email");
    }

    @Test
    @DisplayName("Cok kisa sifre ile register 400 doner")
    void register_withTooShortPassword_returns400() {
        String username = "kisasifre_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = new RegisterRequest(username, username + "@example.com", "kisa");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Refresh token ile yeni access token alinabilir ve eski refresh token artik gecersizdir (rotation)")
    void refresh_withValidToken_rotatesAndReturnsNewTokenPair() {
        String username = "refreshuser_" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                new RegisterRequest(username, username + "@example.com", "Sifre123!"),
                AuthResponse.class);

        String oldRefreshToken = registerResponse.getBody().refreshToken();
        assertThat(oldRefreshToken).isNotBlank();

        ResponseEntity<AuthResponse> refreshResponse = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(oldRefreshToken), AuthResponse.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody().accessToken()).isNotBlank();
        assertThat(refreshResponse.getBody().refreshToken()).isNotEqualTo(oldRefreshToken);


        ResponseEntity<String> reuseAttempt = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(oldRefreshToken), String.class);
        assertThat(reuseAttempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Refresh token gecersiz, refresh 401 doner")
    void refresh_withInvalidToken_returns401() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest("hic-boyle-bir-token-yok"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("REUSE DETECTION: rotate edilmis (eski) bir refresh token tekrar kullanilinca, "
            + "o zincirden uretilmis YENI token da artik gecersiz olur (tum family iptal edildi)")
    void refresh_reuseOfRotatedToken_invalidatesEntireFamily() {
        String username = "reuseuser_" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                new RegisterRequest(username, username + "@example.com", "Sifre123!"),
                AuthResponse.class);

        String originalRefreshToken = registerResponse.getBody().refreshToken();


        ResponseEntity<AuthResponse> firstRefresh = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(originalRefreshToken), AuthResponse.class);
        String rotatedRefreshToken = firstRefresh.getBody().refreshToken();
        assertThat(rotatedRefreshToken).isNotBlank();


        ResponseEntity<String> reuseAttempt = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(originalRefreshToken), String.class);
        assertThat(reuseAttempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);


        ResponseEntity<String> legitimateTokenAfterReuse = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(rotatedRefreshToken), String.class);
        assertThat(legitimateTokenAfterReuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Logout sonrasi ayni refresh token ile yeni access token alinamaz")
    void logout_thenRefresh_returns401() {
        String username = "logoutuser_" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                new RegisterRequest(username, username + "@example.com", "Sifre123!"),
                AuthResponse.class);
        String refreshToken = registerResponse.getBody().refreshToken();

        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
                "/api/auth/logout", new RefreshRequest(refreshToken), Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> refreshAfterLogout = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(refreshToken), String.class);
        assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("5 basarisiz giris denemesinden sonra dogru sifreyle bile giris 429 ile reddedilir")
    void login_afterFiveFailedAttempts_isRateLimited() {
        String username = "kilitlenen_" + UUID.randomUUID().toString().substring(0, 8);
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest(username, username + "@example.com", "DogruSifre123!"),
                AuthResponse.class);


        for (int i = 0; i < 5; i++) {
            restTemplate.postForEntity("/api/auth/login",
                    new LoginRequest(username, "YanlisSifre!"), String.class);
        }


        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(username, "DogruSifre123!"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isNotNull();
    }
}

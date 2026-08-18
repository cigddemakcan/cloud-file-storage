package com.example.filestorage;

import com.example.filestorage.dto.AuthResponse;
import com.example.filestorage.dto.LoginRequest;
import com.example.filestorage.dto.RegisterRequest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.UUID;


final class AuthTestSupport {

    private AuthTestSupport() {
    }

    static String registerAndLogin(TestRestTemplate restTemplate) {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@example.com";
        String password = "Sifre123!";

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                new RegisterRequest(username, email, password),
                AuthResponse.class
        );

        if (registerResponse.getStatusCode().is2xxSuccessful()
                && registerResponse.getBody() != null) {
            return registerResponse.getBody().token();
        }


        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(username, password),
                AuthResponse.class
        );

        return loginResponse.getBody().token();
    }

    static HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    static <T> HttpEntity<T> withAuth(T body, String token) {
        return new HttpEntity<>(body, bearerHeaders(token));
    }
}

package com.example.filestorage.controller;

import com.example.filestorage.dto.AuthResponse;
import com.example.filestorage.dto.LoginRequest;
import com.example.filestorage.dto.RefreshRequest;
import com.example.filestorage.dto.RegisterRequest;
import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.User;
import com.example.filestorage.repository.UserRepository;
import com.example.filestorage.security.CustomUserDetails;
import com.example.filestorage.security.JwtService;
import com.example.filestorage.security.LoginAttemptService;
import com.example.filestorage.service.AuditLogService;
import com.example.filestorage.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest().build();
        }
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().build();
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));


        User saved = userRepository.save(user);
        auditLogService.log(saved.getId(), AuditAction.REGISTER, saved.getId(), null, true, null);

        return ResponseEntity.ok(issueTokenPair(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {


        String rateLimitKey = buildRateLimitKey(request.username(), httpRequest);

        loginAttemptService.assertNotBlocked(rateLimitKey);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (RuntimeException e) {
            loginAttemptService.recordFailure(rateLimitKey);

            auditLogService.log(null, AuditAction.LOGIN, null, null, false,
                    "basarisiz giris denemesi: " + request.username());
            throw e;
        }

        loginAttemptService.recordSuccess(rateLimitKey);

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("Kullanici bulunamadi"));

        auditLogService.log(user.getId(), AuditAction.LOGIN, user.getId(), null, true, null);

        return ResponseEntity.ok(issueTokenPair(user));
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.IssuedRefreshToken issued =
                refreshTokenService.validateAndRotate(request.refreshToken());

        String accessToken = jwtService.generateToken(new CustomUserDetails(issued.user()));

        return ResponseEntity.ok(new AuthResponse(
                accessToken, issued.rawToken(), TOKEN_TYPE, jwtService.getExpirationSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.createFor(user);
        return new AuthResponse(accessToken, issued.rawToken(), TOKEN_TYPE, jwtService.getExpirationSeconds());
    }

    private String buildRateLimitKey(String username, HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);

        return username.trim().toLowerCase() + "|" + ip;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

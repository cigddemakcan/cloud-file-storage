package com.example.filestorage.repository;

import com.example.filestorage.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);


    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
    void revokeAllForUser(@Param("userId") Long userId);


    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.tokenFamily = :family AND r.revoked = false")
    void revokeAllInFamily(@Param("family") String tokenFamily);
}

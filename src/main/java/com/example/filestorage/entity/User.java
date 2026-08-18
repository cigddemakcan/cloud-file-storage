package com.example.filestorage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;


    @Column(nullable = false)
    private long storageQuota = 1_073_741_824L;

    @Column(nullable = false)
    private long usedStorage = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum Role {
        USER, ADMIN
    }
}

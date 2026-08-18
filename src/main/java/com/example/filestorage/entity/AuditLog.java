package com.example.filestorage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;


    @Column
    private Long targetId;

    @Column
    private String ipAddress;

    @Column(nullable = false)
    private boolean success = true;


    @Column(length = 500)
    private String detail;

    @Column(nullable = false, updatable = false)
    private Instant timestamp = Instant.now();
}

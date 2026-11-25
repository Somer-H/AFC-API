package com.afcgym.platform.persistance.entities;

import com.afcgym.platform.persistance.entities.enums.AuditOutcome;
import com.afcgym.platform.persistance.entities.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_log")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id", updatable = false)
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", length = 20, updatable = false)
    private Role actorRole;

    @Column(nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "http_method", nullable = false, length = 10, updatable = false)
    private String httpMethod;

    @Column(nullable = false, length = 300, updatable = false)
    private String path;

    @Column(name = "target_id", length = 50, updatable = false)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AuditOutcome outcome;

    @Column(name = "response_status", nullable = false, updatable = false)
    private int responseStatus;
}

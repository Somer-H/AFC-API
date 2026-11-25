package com.afcgym.platform.persistance.entities;

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
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "credentials_changed_at")
    private Instant credentialsChangedAt;

    public void deactivate() {
        setActive(false);
    }

    public void changePassword(String newPasswordHash, Instant changedAt) {
        setPasswordHash(newPasswordHash);
        setCredentialsChangedAt(changedAt);
    }

    public boolean credentialsChangedAfter(Instant moment) {
        return credentialsChangedAt != null && moment.isBefore(credentialsChangedAt.truncatedTo(ChronoUnit.SECONDS));
    }
}

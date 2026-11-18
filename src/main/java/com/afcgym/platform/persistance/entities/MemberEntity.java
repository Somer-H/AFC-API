package com.afcgym.platform.persistance.entities;

import com.afcgym.platform.persistance.entities.enums.MembershipType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "member")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Setter(AccessLevel.PRIVATE)
    @Column(length = 20)
    private String phone;

    @Setter(AccessLevel.PRIVATE)
    @Column(length = 120)
    private String email;

    @Setter(AccessLevel.PRIVATE)
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false, length = 20)
    private MembershipType membershipType;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "pin_hash", length = 100)
    private String pinHash;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "fingerprint_token", length = 100)
    private String fingerprintToken;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false)
    private boolean active;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public void assignCode(String code) {
        setCode(code);
    }

    public void updateProfile(String fullName, String phone, String email, MembershipType membershipType) {
        setFullName(fullName);
        setPhone(phone);
        setEmail(email);
        setMembershipType(membershipType);
    }

    public void renew(MembershipType membershipType, LocalDate dueDate) {
        setMembershipType(membershipType);
        setDueDate(dueDate);
    }

    public void updateCredentials(String pinHash, String fingerprintToken) {
        if (pinHash != null) {
            setPinHash(pinHash);
        }
        if (fingerprintToken != null) {
            setFingerprintToken(fingerprintToken);
        }
    }

    public void deactivate() {
        setActive(false);
    }

    public void linkAccount(Long userId) {
        setUserId(userId);
    }
}

package com.afcgym.platform.persistance.entities;

import com.afcgym.platform.persistance.entities.enums.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_appointment")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentAppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Setter(AccessLevel.PRIVATE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "suggested_at")
    private LocalDateTime suggestedAt;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "decided_by")
    private Long decidedBy;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public void accept(Instant decidedAt, Long decidedBy) {
        setStatus(AppointmentStatus.ACCEPTED);
        setDecidedAt(decidedAt);
        setDecidedBy(decidedBy);
    }

    public void cancel(Instant decidedAt, Long decidedBy, String reason, LocalDateTime suggestedAt) {
        setStatus(AppointmentStatus.CANCELLED);
        setDecidedAt(decidedAt);
        setDecidedBy(decidedBy);
        setCancellationReason(reason);
        setSuggestedAt(suggestedAt);
    }

    public boolean isActive() {
        return status == AppointmentStatus.PENDING || status == AppointmentStatus.ACCEPTED;
    }
}

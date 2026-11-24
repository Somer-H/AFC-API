package com.afcgym.platform.persistance.entities;

import com.afcgym.platform.persistance.entities.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "booking")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_block_id", nullable = false)
    private ScheduleBlockEntity scheduleBlock;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @Setter(AccessLevel.PRIVATE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public boolean isActive() {
        return status == BookingStatus.ACTIVE;
    }

    public void cancel(Instant cancelledAt) {
        setStatus(BookingStatus.CANCELLED);
        setCancelledAt(cancelledAt);
    }
}

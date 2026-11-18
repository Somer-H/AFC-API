package com.afcgym.platform.persistance.entities;

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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "schedule_block")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleBlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.PRIVATE)
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity;

    @Setter(AccessLevel.PRIVATE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private CoachEntity coach;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public void updateDetails(
            DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, int maxCapacity, CoachEntity coach) {
        setDayOfWeek(dayOfWeek);
        setStartTime(startTime);
        setEndTime(endTime);
        setMaxCapacity(maxCapacity);
        setCoach(coach);
    }

    public void activate() {
        setActive(true);
    }

    public void deactivate() {
        setActive(false);
    }
}

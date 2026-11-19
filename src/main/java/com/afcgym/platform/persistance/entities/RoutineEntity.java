package com.afcgym.platform.persistance.entities;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "routine")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoutineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.PRIVATE)
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, unique = true, length = 10)
    private DayOfWeek dayOfWeek;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false, length = 150)
    private String title;

    @Setter(AccessLevel.PRIVATE)
    @Column(length = 1000)
    private String description;

    @Setter(AccessLevel.PRIVATE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private CoachEntity coach;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    @OrderBy("sortOrder ASC")
    private List<ExerciseEntity> exercises = new ArrayList<>();

    public void updateDetails(DayOfWeek dayOfWeek, String title, String description, CoachEntity coach) {
        setDayOfWeek(dayOfWeek);
        setTitle(title);
        setDescription(description);
        setCoach(coach);
    }

    public void replaceExercises(List<ExerciseEntity> newExercises) {
        exercises.clear();
        exercises.addAll(newExercises);
    }
}

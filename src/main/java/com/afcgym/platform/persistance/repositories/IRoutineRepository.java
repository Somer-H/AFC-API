package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.RoutineEntity;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IRoutineRepository extends JpaRepository<@NonNull RoutineEntity, @NonNull Long> {

    boolean existsByDayOfWeek(DayOfWeek dayOfWeek);

    @Override
    @EntityGraph(attributePaths = {"coach", "exercises"})
    @NonNull
    List<RoutineEntity> findAll();

    @EntityGraph(attributePaths = {"coach", "exercises"})
    Optional<RoutineEntity> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"coach", "exercises"})
    Optional<RoutineEntity> findByDayOfWeek(DayOfWeek dayOfWeek);
}

package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.ScheduleBlockEntity;
import jakarta.persistence.LockModeType;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface IScheduleBlockRepository extends JpaRepository<@NonNull ScheduleBlockEntity, @NonNull Long> {

    @Override
    @EntityGraph(attributePaths = "coach")
    @NonNull
    List<ScheduleBlockEntity> findAll();

    @EntityGraph(attributePaths = "coach")
    List<ScheduleBlockEntity> findByDayOfWeekOrderByStartTimeAsc(DayOfWeek dayOfWeek);

    @EntityGraph(attributePaths = "coach")
    Optional<ScheduleBlockEntity> findWithCoachById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ScheduleBlockEntity> findWithLockById(Long id);
}

package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.BookingEntity;
import com.afcgym.platform.persistance.entities.enums.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IBookingRepository extends JpaRepository<@NonNull BookingEntity, @NonNull Long> {

    long countByScheduleBlockIdAndClassDateAndStatus(Long scheduleBlockId, LocalDate classDate, BookingStatus status);

    boolean existsByScheduleBlockIdAndMemberIdAndClassDateAndStatus(
            Long scheduleBlockId, Long memberId, LocalDate classDate, BookingStatus status);

    List<BookingEntity> findByClassDateAndStatus(LocalDate classDate, BookingStatus status);

    List<BookingEntity> findByScheduleBlockIdAndClassDateGreaterThanEqualAndStatus(
            Long scheduleBlockId, LocalDate from, BookingStatus status);

    @EntityGraph(attributePaths = "scheduleBlock")
    List<BookingEntity> findByMemberIdAndClassDateGreaterThanEqualAndStatus(
            Long memberId, LocalDate from, BookingStatus status);

    @EntityGraph(attributePaths = "scheduleBlock")
    Optional<BookingEntity> findWithScheduleBlockById(Long id);
}

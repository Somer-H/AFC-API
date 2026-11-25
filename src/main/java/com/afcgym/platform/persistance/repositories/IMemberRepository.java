package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.MemberEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface IMemberRepository extends
        JpaRepository<@NonNull MemberEntity, @NonNull Long>,
        JpaSpecificationExecutor<@NonNull MemberEntity> {

    boolean existsByCode(String code);

    Optional<MemberEntity> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MemberEntity> findWithLockById(Long id);

    long countByActiveTrueAndDueDateGreaterThanEqual(LocalDate dueDate);

    long countByActiveTrueAndDueDateBetween(LocalDate from, LocalDate to);

    long countByActiveTrueAndDueDateLessThan(LocalDate dueDate);

    long countByCreatedAtGreaterThanEqual(Instant createdAt);

    List<MemberEntity> findByActiveTrueAndDueDateBetweenOrderByDueDateAsc(LocalDate from, LocalDate to);
}

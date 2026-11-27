package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface IPaymentRepository extends JpaRepository<@NonNull PaymentEntity, @NonNull Long> {

    List<PaymentEntity> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    List<PaymentEntity> findByStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(
            PaymentStatus status, Instant from, Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentEntity> findWithLockById(Long id);

    boolean existsByMemberIdAndStatusIn(Long memberId, Collection<PaymentStatus> statuses);
}

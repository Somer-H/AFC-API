package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPaymentRepository extends JpaRepository<@NonNull PaymentEntity, @NonNull Long> {

    List<PaymentEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<PaymentEntity> findByStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(
            PaymentStatus status, Instant from, Instant to);
}

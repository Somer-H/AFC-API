package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.PaymentAppointmentEntity;
import com.afcgym.platform.persistance.entities.enums.AppointmentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface IPaymentAppointmentRepository extends JpaRepository<@NonNull PaymentAppointmentEntity, @NonNull Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentAppointmentEntity> findWithLockById(Long id);

    List<PaymentAppointmentEntity> findByPaymentIdInOrderByIdAsc(Collection<Long> paymentIds);

    List<PaymentAppointmentEntity> findByPaymentIdAndStatusIn(Long paymentId, Collection<AppointmentStatus> statuses);

    List<PaymentAppointmentEntity> findByStatusAndScheduledAtGreaterThanAndScheduledAtLessThan(
            AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    List<PaymentAppointmentEntity> findByStatusOrderByScheduledAtAsc(AppointmentStatus status);

    List<PaymentAppointmentEntity> findByStatusAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
            AppointmentStatus status, LocalDateTime from);
}

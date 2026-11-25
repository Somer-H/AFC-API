package com.afcgym.platform.service.impl;

import com.afcgym.platform.configuration.PaymentProperties;
import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.PaymentAppointmentEntity;
import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.persistance.entities.enums.AppointmentStatus;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IPaymentAppointmentRepository;
import com.afcgym.platform.persistance.repositories.IPaymentRepository;
import com.afcgym.platform.service.IPaymentAppointmentService;
import com.afcgym.platform.service.dto.AppointmentAcceptedEvent;
import com.afcgym.platform.service.dto.AppointmentAgendaItem;
import com.afcgym.platform.service.dto.AppointmentCancelledEvent;
import com.afcgym.platform.service.dto.CancelAppointmentCommand;
import com.afcgym.platform.utils.SpanishDateFormatter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentAppointmentServiceImpl implements IPaymentAppointmentService {

    private final IPaymentAppointmentRepository appointmentRepository;
    private final IPaymentRepository paymentRepository;
    private final IMemberRepository memberRepository;
    private final PaymentProperties paymentProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<AppointmentAgendaItem> listAgenda(LocalDate today) {
        List<PaymentAppointmentEntity> appointments = Stream.concat(
                        appointmentRepository.findByStatusOrderByScheduledAtAsc(AppointmentStatus.PENDING).stream(),
                        appointmentRepository.findByStatusAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
                                AppointmentStatus.ACCEPTED, today.atStartOfDay()).stream())
                .sorted(Comparator.comparing(PaymentAppointmentEntity::getScheduledAt)
                        .thenComparing(PaymentAppointmentEntity::getId))
                .toList();
        if (appointments.isEmpty()) {
            return List.of();
        }
        Map<Long, PaymentEntity> payments = paymentRepository
                .findAllById(appointments.stream().map(PaymentAppointmentEntity::getPaymentId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(PaymentEntity::getId, Function.identity()));
        Map<Long, MemberEntity> members = memberRepository
                .findAllById(appointments.stream().map(PaymentAppointmentEntity::getMemberId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(MemberEntity::getId, Function.identity()));
        return appointments.stream()
                .map(appointment -> new AppointmentAgendaItem(appointment,
                        payments.get(appointment.getPaymentId()), members.get(appointment.getMemberId())))
                .toList();
    }

    @Override
    @Transactional
    public PaymentAppointmentEntity accept(Long appointmentId, Long adminUserId) {
        PaymentAppointmentEntity appointment = lockAppointment(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Solo se pueden aceptar citas pendientes");
        }
        if (!appointment.getScheduledAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("La cita ya pasó; cancélala para que el socio elija otro horario");
        }
        Duration length = Duration.ofMinutes(paymentProperties.appointmentMinutes());
        List<PaymentAppointmentEntity> overlapping = appointmentRepository
                .findByStatusAndScheduledAtGreaterThanAndScheduledAtLessThan(AppointmentStatus.ACCEPTED,
                        appointment.getScheduledAt().minus(length), appointment.getScheduledAt().plus(length));
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Ya hay una cita aceptada a las "
                    + SpanishDateFormatter.time(overlapping.getFirst().getScheduledAt())
                    + " y cada cita dura " + paymentProperties.appointmentMinutes() + " minutos");
        }
        PaymentEntity payment = lockPayment(appointment.getPaymentId());
        if (payment.getStatus() != PaymentStatus.PENDING_APPOINTMENT) {
            throw new BadRequestException("El pago de esta cita ya no está pendiente");
        }
        appointment.accept(Instant.now(), adminUserId);
        payment.confirmAppointment();
        eventPublisher.publishEvent(new AppointmentAcceptedEvent(appointment.getMemberId(), appointment.getScheduledAt()));
        return appointment;
    }

    @Override
    @Transactional
    public PaymentAppointmentEntity cancel(Long appointmentId, CancelAppointmentCommand command, Long adminUserId) {
        String reason = command.reason() == null || command.reason().isBlank() ? null : command.reason().strip();
        if (command.suggestedAt() != null && !command.suggestedAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("El horario sugerido debe ser en el futuro");
        }
        PaymentAppointmentEntity appointment = lockAppointment(appointmentId);
        if (!appointment.isActive()) {
            throw new BadRequestException("La cita ya estaba cancelada");
        }
        PaymentEntity payment = lockPayment(appointment.getPaymentId());
        appointment.cancel(Instant.now(), adminUserId, reason, command.suggestedAt());
        if (payment.getStatus().isWaitingForAppointment()) {
            payment.markAppointmentCancelled();
        }
        eventPublisher.publishEvent(new AppointmentCancelledEvent(appointment.getMemberId(), reason, command.suggestedAt()));
        return appointment;
    }

    private PaymentAppointmentEntity lockAppointment(Long appointmentId) {
        return appointmentRepository.findWithLockById(appointmentId)
                .orElseThrow(() -> new NotFoundException("No se encontró la cita solicitada"));
    }

    private PaymentEntity lockPayment(Long paymentId) {
        return paymentRepository.findWithLockById(paymentId)
                .orElseThrow(() -> new NotFoundException("No se encontró el pago solicitado"));
    }
}

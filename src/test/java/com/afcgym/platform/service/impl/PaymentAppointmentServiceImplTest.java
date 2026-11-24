package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.afcgym.platform.configuration.PaymentProperties;
import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.PaymentAppointmentEntity;
import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.persistance.entities.enums.AppointmentStatus;
import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IPaymentAppointmentRepository;
import com.afcgym.platform.persistance.repositories.IPaymentRepository;
import com.afcgym.platform.service.dto.AppointmentAcceptedEvent;
import com.afcgym.platform.service.dto.AppointmentAgendaItem;
import com.afcgym.platform.service.dto.AppointmentCancelledEvent;
import com.afcgym.platform.service.dto.CancelAppointmentCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentAppointmentServiceImplTest {

    @Mock
    private IPaymentAppointmentRepository appointmentRepository;

    @Mock
    private IPaymentRepository paymentRepository;

    @Mock
    private IMemberRepository memberRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentAppointmentServiceImpl appointmentService;

    private LocalDate today;

    private LocalDateTime tomorrowEvening;

    @BeforeEach
    void init() {
        PaymentProperties paymentProperties = new PaymentProperties(30, 30,
                new PaymentProperties.BankTransfer(null, null, null),
                new PaymentProperties.MercadoPago("https://mp.test", null, null, null, null));
        appointmentService = new PaymentAppointmentServiceImpl(appointmentRepository, paymentRepository,
                memberRepository, paymentProperties, eventPublisher);
        today = LocalDate.now();
        tomorrowEvening = today.plusDays(1).atTime(18, 0);
    }

    @Test
    void givenPendingAndAcceptedAppointments_whenListAgenda_thenMergeThemByTimeWithMemberAndPayment() {
        PaymentAppointmentEntity laterPending = createAppointmentDummy(1L, 10L, tomorrowEvening.plusHours(2), AppointmentStatus.PENDING);
        PaymentAppointmentEntity earlierAccepted = createAppointmentDummy(2L, 11L, tomorrowEvening, AppointmentStatus.ACCEPTED);
        when(appointmentRepository.findByStatusOrderByScheduledAtAsc(AppointmentStatus.PENDING)).thenReturn(List.of(laterPending));
        when(appointmentRepository.findByStatusAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
                AppointmentStatus.ACCEPTED, today.atStartOfDay())).thenReturn(List.of(earlierAccepted));
        when(paymentRepository.findAllById(any())).thenReturn(List.of(
                createPaymentDummy(10L, PaymentStatus.PENDING_APPOINTMENT),
                createPaymentDummy(11L, PaymentStatus.APPOINTMENT_CONFIRMED)));
        when(memberRepository.findAllById(any())).thenReturn(List.of(createMemberDummy()));

        List<AppointmentAgendaItem> agenda = appointmentService.listAgenda(today);

        assertEquals(2, agenda.size());
        assertEquals(2L, agenda.get(0).appointment().getId());
        assertEquals(11L, agenda.get(0).payment().getId());
        assertEquals("Ana Efectivo", agenda.get(1).member().getFullName());
    }

    @Test
    void givenNoAppointments_whenListAgenda_thenReturnAnEmptyAgendaWithoutLoadingPaymentsOrMembers() {
        when(appointmentRepository.findByStatusOrderByScheduledAtAsc(AppointmentStatus.PENDING)).thenReturn(List.of());
        when(appointmentRepository.findByStatusAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
                AppointmentStatus.ACCEPTED, today.atStartOfDay())).thenReturn(List.of());

        List<AppointmentAgendaItem> agenda = appointmentService.listAgenda(today);

        verifyNoInteractions(paymentRepository, memberRepository);
        assertTrue(agenda.isEmpty());
    }

    @Test
    void givenAPendingAppointmentWithAFreeSlot_whenAccept_thenAcceptItConfirmThePaymentAndNotify() {
        PaymentAppointmentEntity appointment = createAppointmentDummy(1L, 10L, tomorrowEvening, AppointmentStatus.PENDING);
        PaymentEntity payment = createPaymentDummy(10L, PaymentStatus.PENDING_APPOINTMENT);
        when(appointmentRepository.findWithLockById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.findByStatusAndScheduledAtGreaterThanAndScheduledAtLessThan(
                AppointmentStatus.ACCEPTED, tomorrowEvening.minusMinutes(30), tomorrowEvening.plusMinutes(30)))
                .thenReturn(List.of());
        when(paymentRepository.findWithLockById(10L)).thenReturn(Optional.of(payment));

        PaymentAppointmentEntity result = appointmentService.accept(1L, 99L);

        verify(eventPublisher).publishEvent(new AppointmentAcceptedEvent(1L, tomorrowEvening));
        assertEquals(AppointmentStatus.ACCEPTED, result.getStatus());
        assertEquals(99L, result.getDecidedBy());
        assertEquals(PaymentStatus.APPOINTMENT_CONFIRMED, payment.getStatus());
    }

    @Test
    void givenAnotherAcceptedAppointmentWithinThirtyMinutes_whenAccept_thenThrowConflictException() {
        PaymentAppointmentEntity appointment = createAppointmentDummy(1L, 10L, tomorrowEvening, AppointmentStatus.PENDING);
        PaymentAppointmentEntity booked = createAppointmentDummy(2L, 11L, tomorrowEvening.minusMinutes(15), AppointmentStatus.ACCEPTED);
        when(appointmentRepository.findWithLockById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.findByStatusAndScheduledAtGreaterThanAndScheduledAtLessThan(
                AppointmentStatus.ACCEPTED, tomorrowEvening.minusMinutes(30), tomorrowEvening.plusMinutes(30)))
                .thenReturn(List.of(booked));

        ConflictException exception = assertThrows(ConflictException.class, () -> appointmentService.accept(1L, 99L));

        verify(paymentRepository, never()).findWithLockById(any());
        verifyNoInteractions(eventPublisher);
        assertEquals("Ya hay una cita aceptada a las 17:45 y cada cita dura 30 minutos", exception.getMessage());
    }

    @Test
    void givenAnAppointmentThatAlreadyPassed_whenAccept_thenThrowBadRequestException() {
        when(appointmentRepository.findWithLockById(1L)).thenReturn(Optional.of(
                createAppointmentDummy(1L, 10L, LocalDateTime.now().minusHours(1), AppointmentStatus.PENDING)));

        assertThrows(BadRequestException.class, () -> appointmentService.accept(1L, 99L));

        verifyNoInteractions(paymentRepository, eventPublisher);
    }

    @Test
    void givenAnAcceptedAppointment_whenAcceptAgain_thenThrowBadRequestException() {
        when(appointmentRepository.findWithLockById(1L)).thenReturn(Optional.of(
                createAppointmentDummy(1L, 10L, tomorrowEvening, AppointmentStatus.ACCEPTED)));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> appointmentService.accept(1L, 99L));

        assertEquals("Solo se pueden aceptar citas pendientes", exception.getMessage());
    }

    @Test
    void givenAnUnknownAppointment_whenAccept_thenThrowNotFoundException() {
        when(appointmentRepository.findWithLockById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> appointmentService.accept(1L, 99L));
    }

    @Test
    void givenAnAcceptedAppointment_whenCancelWithReasonAndSuggestion_thenCancelItReopenThePaymentAndNotify() {
        PaymentAppointmentEntity appointment = createAppointmentDummy(1L, 10L, tomorrowEvening, AppointmentStatus.ACCEPTED);
        PaymentEntity payment = createPaymentDummy(10L, PaymentStatus.APPOINTMENT_CONFIRMED);
        LocalDateTime suggestion = tomorrowEvening.plusDays(1);
        when(appointmentRepository.findWithLockById(1L)).thenReturn(Optional.of(appointment));
        when(paymentRepository.findWithLockById(10L)).thenReturn(Optional.of(payment));

        PaymentAppointmentEntity result = appointmentService.cancel(1L,
                new CancelAppointmentCommand("  Recepción cerrada  ", suggestion), 99L);

        verify(eventPublisher).publishEvent(new AppointmentCancelledEvent(1L, "Recepción cerrada", suggestion));
        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertEquals("Recepción cerrada", result.getCancellationReason());
        assertEquals(suggestion, result.getSuggestedAt());
        assertEquals(PaymentStatus.APPOINTMENT_CANCELLED, payment.getStatus());
    }

    @Test
    void givenASuggestedTimeInThePast_whenCancel_thenThrowBadRequestWithoutLoadingTheAppointment() {
        assertThrows(BadRequestException.class, () -> appointmentService.cancel(1L,
                new CancelAppointmentCommand(null, LocalDateTime.now().minusDays(1)), 99L));

        verifyNoInteractions(appointmentRepository, paymentRepository, eventPublisher);
    }

    @Test
    void givenACancelledAppointment_whenCancelAgain_thenThrowBadRequestException() {
        when(appointmentRepository.findWithLockById(1L)).thenReturn(Optional.of(
                createAppointmentDummy(1L, 10L, tomorrowEvening, AppointmentStatus.CANCELLED)));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> appointmentService.cancel(1L, new CancelAppointmentCommand(null, null), 99L));

        verifyNoInteractions(paymentRepository, eventPublisher);
        assertEquals("La cita ya estaba cancelada", exception.getMessage());
    }

    private PaymentAppointmentEntity createAppointmentDummy(Long id, Long paymentId, LocalDateTime scheduledAt,
                                                           AppointmentStatus status) {
        return PaymentAppointmentEntity.builder()
                .id(id)
                .paymentId(paymentId)
                .memberId(1L)
                .scheduledAt(scheduledAt)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    private PaymentEntity createPaymentDummy(Long id, PaymentStatus status) {
        return PaymentEntity.builder()
                .id(id)
                .memberId(1L)
                .membershipType(MembershipType.MONTHLY)
                .method(PaymentMethod.CASH)
                .status(status)
                .amount(new BigDecimal("500.00"))
                .createdAt(Instant.now())
                .build();
    }

    private MemberEntity createMemberDummy() {
        return MemberEntity.builder()
                .id(1L)
                .code("SOC-000001")
                .fullName("Ana Efectivo")
                .phone("5512345678")
                .membershipType(MembershipType.MONTHLY)
                .dueDate(today.plusDays(2))
                .active(true)
                .createdAt(Instant.now())
                .build();
    }
}

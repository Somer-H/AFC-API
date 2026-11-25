package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.afcgym.platform.configuration.PaymentProperties;
import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.ForbiddenException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.exception.OwnershipViolationException;
import com.afcgym.platform.exception.ServiceUnavailableException;
import com.afcgym.platform.exception.UnauthorizedException;
import com.afcgym.platform.persistance.entities.CheckInEntity;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.PaymentAppointmentEntity;
import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.persistance.entities.enums.AppointmentStatus;
import com.afcgym.platform.persistance.entities.enums.AuditOutcome;
import com.afcgym.platform.persistance.entities.enums.CheckInMethod;
import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.ICheckInRepository;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IPaymentAppointmentRepository;
import com.afcgym.platform.persistance.repositories.IPaymentRepository;
import com.afcgym.platform.service.IAuditService;
import com.afcgym.platform.service.IMemberService;
import com.afcgym.platform.service.IPaymentGatewayService;
import com.afcgym.platform.service.dto.AuditEntry;
import com.afcgym.platform.service.dto.GatewayCheckout;
import com.afcgym.platform.service.dto.GatewayNotification;
import com.afcgym.platform.service.dto.GatewayPayment;
import com.afcgym.platform.service.dto.MemberPaymentView;
import com.afcgym.platform.service.dto.MembershipRenewedEvent;
import com.afcgym.platform.service.dto.PaymentRejectedEvent;
import com.afcgym.platform.service.dto.RenewMembershipCommand;
import com.afcgym.platform.service.dto.Requester;
import com.afcgym.platform.service.dto.StartMemberPaymentCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final GatewayNotification SIGNED_NOTIFICATION =
            new GatewayNotification("payment", "123456", "request-1", "ts=1700000000,v1=abc");

    @Mock
    private IPaymentRepository paymentRepository;

    @Mock
    private IPaymentAppointmentRepository appointmentRepository;

    @Mock
    private ICheckInRepository checkInRepository;

    @Mock
    private IMemberRepository memberRepository;

    @Mock
    private IMemberService memberService;

    @Mock
    private IPaymentGatewayService paymentGatewayService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private IAuditService auditService;

    private PaymentServiceImpl paymentService;

    private MemberEntity existingMember;

    private Requester socio;

    private LocalDateTime tomorrowEvening;

    @BeforeEach
    void init() {
        PaymentProperties paymentProperties = new PaymentProperties(30, 30,
                new PaymentProperties.BankTransfer("Banco de pruebas", "AFC Gym", "012345678901234567"),
                new PaymentProperties.MercadoPago("https://mp.test", "TEST-token", "secret", null, null));
        paymentService = new PaymentServiceImpl(paymentRepository, appointmentRepository, checkInRepository,
                memberRepository, memberService, paymentGatewayService, paymentProperties, eventPublisher, auditService);
        existingMember = MemberEntity.builder()
                .id(1L)
                .code("SOC-000001")
                .fullName("Ana Test")
                .membershipType(MembershipType.MONTHLY)
                .dueDate(LocalDate.now().plusDays(5))
                .active(true)
                .createdAt(Instant.now())
                .build();
        socio = new Requester(10L, Role.SOCIO);
        tomorrowEvening = LocalDate.now().plusDays(1).atTime(18, 0);
    }

    @Test
    void givenACardPayment_whenRenew_thenApproveInstantlyAndRenewTheMembership() {
        when(memberService.getById(1L)).thenReturn(existingMember);
        when(checkInRepository.findByMemberId(1L)).thenReturn(List.of());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RenewMembershipCommand command = new RenewMembershipCommand(MembershipType.MONTHLY, PaymentMethod.CARD, "1234", null);

        PaymentEntity result = paymentService.renew(1L, command);

        verify(memberService).renew(1L, MembershipType.MONTHLY);
        verify(eventPublisher).publishEvent(new MembershipRenewedEvent(1L));
        assertEquals(PaymentStatus.APPROVED, result.getStatus());
        assertEquals(0, result.getDiscountPercent());
    }

    @Test
    void givenASpeiPayment_whenRenew_thenWaitForTheBankValidationWithoutRenewing() {
        when(memberService.getById(1L)).thenReturn(existingMember);
        when(checkInRepository.findByMemberId(1L)).thenReturn(List.of());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RenewMembershipCommand command = new RenewMembershipCommand(MembershipType.MONTHLY, PaymentMethod.SPEI, null, null);

        PaymentEntity result = paymentService.renew(1L, command);

        verify(memberService, never()).renew(any(), any());
        verifyNoInteractions(eventPublisher);
        assertEquals(PaymentStatus.PENDING_BANK_VALIDATION, result.getStatus());
    }

    @Test
    void givenACardPaymentWithoutLastFourDigits_whenRenew_thenThrowBadRequestException() {
        when(memberService.getById(1L)).thenReturn(existingMember);
        RenewMembershipCommand command = new RenewMembershipCommand(MembershipType.MONTHLY, PaymentMethod.CARD, null, null);

        assertThrows(BadRequestException.class, () -> paymentService.renew(1L, command));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void givenACashPaymentWithoutScheduledDate_whenRenew_thenThrowBadRequestException() {
        when(memberService.getById(1L)).thenReturn(existingMember);
        RenewMembershipCommand command = new RenewMembershipCommand(MembershipType.MONTHLY, PaymentMethod.CASH, null, null);

        assertThrows(BadRequestException.class, () -> paymentService.renew(1L, command));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void givenAnEightyDayStreak_whenRenew_thenApplyTenPercentDiscount() {
        when(memberService.getById(1L)).thenReturn(existingMember);
        when(checkInRepository.findByMemberId(1L)).thenReturn(buildCheckInStreak(80));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RenewMembershipCommand command = new RenewMembershipCommand(MembershipType.MONTHLY, PaymentMethod.CARD, "1234", null);

        PaymentEntity result = paymentService.renew(1L, command);

        assertEquals(10, result.getDiscountPercent());
        assertEquals(0, new BigDecimal("450.00").compareTo(result.getAmount()));
    }

    @Test
    void givenAnUnknownMember_whenListByMember_thenThrowNotFoundException() {
        when(memberService.getById(99L)).thenThrow(new NotFoundException("No se encontró el socio solicitado"));

        assertThrows(NotFoundException.class, () -> paymentService.listByMember(99L));
    }

    @Test
    void givenATransferWaitingForValidation_whenApprove_thenApproveRecordTheAdminAndRenew() {
        PaymentEntity transfer = payment(10L, PaymentMethod.SPEI, PaymentStatus.PENDING_BANK_VALIDATION);
        when(paymentRepository.findWithLockById(10L)).thenReturn(Optional.of(transfer));

        PaymentEntity result = paymentService.approve(10L, 99L);

        verify(memberService).renew(1L, MembershipType.MONTHLY);
        verify(eventPublisher).publishEvent(new MembershipRenewedEvent(1L));
        assertEquals(PaymentStatus.APPROVED, result.getStatus());
        assertEquals(99L, result.getApprovedBy());
        assertNotNull(result.getApprovedAt());
    }

    @Test
    void givenAnAlreadyApprovedPayment_whenApprove_thenThrowBadRequestException() {
        when(paymentRepository.findWithLockById(11L))
                .thenReturn(Optional.of(payment(11L, PaymentMethod.CARD, PaymentStatus.APPROVED)));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> paymentService.approve(11L, 99L));

        verify(memberService, never()).renew(any(), any());
        assertEquals("El pago ya fue procesado", exception.getMessage());
    }

    @Test
    void givenAnUnknownPayment_whenApprove_thenThrowNotFoundException() {
        when(paymentRepository.findWithLockById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> paymentService.approve(99L, 99L));
    }

    @Test
    void givenAnOnlineCardPayment_whenApprove_thenThrowBadRequestBecauseOnlyMercadoPagoConfirmsIt() {
        when(paymentRepository.findWithLockById(12L)).thenReturn(Optional.of(onlineCardPayment(12L)));

        assertThrows(BadRequestException.class, () -> paymentService.approve(12L, 99L));

        verify(memberService, never()).renew(any(), any());
    }

    @Test
    void givenATransferWaitingForValidation_whenReject_thenRejectWithTheReasonAndNotifyTheMember() {
        PaymentEntity transfer = payment(10L, PaymentMethod.SPEI, PaymentStatus.PENDING_BANK_VALIDATION);
        when(paymentRepository.findWithLockById(10L)).thenReturn(Optional.of(transfer));

        PaymentEntity result = paymentService.reject(10L, "  No aparece el depósito  ", 99L);

        verify(eventPublisher).publishEvent(new PaymentRejectedEvent(1L, "No aparece el depósito"));
        verify(memberService, never()).renew(any(), any());
        assertEquals(PaymentStatus.REJECTED, result.getStatus());
        assertEquals("No aparece el depósito", result.getRejectionReason());
        assertEquals(99L, result.getRejectedBy());
    }

    @Test
    void givenABlankReason_whenReject_thenThrowBadRequestWithoutLoadingThePayment() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> paymentService.reject(10L, "   ", 99L));

        verifyNoInteractions(paymentRepository);
        assertEquals("Escribe el motivo del rechazo", exception.getMessage());
    }

    @Test
    void givenACashPayment_whenReject_thenThrowBadRequestException() {
        when(paymentRepository.findWithLockById(10L))
                .thenReturn(Optional.of(payment(10L, PaymentMethod.CASH, PaymentStatus.PENDING_APPOINTMENT)));

        assertThrows(BadRequestException.class, () -> paymentService.reject(10L, "No llegó", 99L));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenACardRenewal_whenStartForMember_thenCreateTheMercadoPagoCheckout() {
        givenAMemberWithoutOpenPayments();
        when(paymentGatewayService.isConfigured()).thenReturn(true);
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));
        when(paymentGatewayService.createCheckout(any(PaymentEntity.class)))
                .thenReturn(new GatewayCheckout("pref-7", "https://checkout.test/pref-7"));

        MemberPaymentView view = paymentService.startForMember(
                new StartMemberPaymentCommand(MembershipType.MONTHLY, PaymentMethod.CARD, null), socio);

        assertEquals(PaymentStatus.AWAITING_GATEWAY, view.payment().getStatus());
        assertEquals("https://checkout.test/pref-7", view.payment().getCheckoutUrl());
        assertEquals(0, new BigDecimal("500.00").compareTo(view.payment().getAmount()));
        assertNull(view.bankTransfer());
    }

    @Test
    void givenATransfer_whenStartForMember_thenReturnTheBankDetailsWithTheReference() {
        givenAMemberWithoutOpenPayments();
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));

        MemberPaymentView view = paymentService.startForMember(
                new StartMemberPaymentCommand(MembershipType.WEEKLY, PaymentMethod.SPEI, null), socio);

        verifyNoInteractions(paymentGatewayService, appointmentRepository);
        assertEquals(PaymentStatus.PENDING_BANK_VALIDATION, view.payment().getStatus());
        assertEquals("012345678901234567", view.bankTransfer().clabe());
        assertEquals("PAG-000007", view.bankTransfer().reference());
    }

    @Test
    void givenACashRenewal_whenStartForMember_thenRequestAnAppointment() {
        givenAMemberWithoutOpenPayments();
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));
        when(appointmentRepository.save(any(PaymentAppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MemberPaymentView view = paymentService.startForMember(
                new StartMemberPaymentCommand(MembershipType.MONTHLY, PaymentMethod.CASH, tomorrowEvening), socio);

        assertEquals(PaymentStatus.PENDING_APPOINTMENT, view.payment().getStatus());
        assertEquals(AppointmentStatus.PENDING, view.appointment().getStatus());
        assertEquals(tomorrowEvening, view.appointment().getScheduledAt());
        assertEquals(7L, view.appointment().getPaymentId());
    }

    @Test
    void givenACashRenewalInThePast_whenStartForMember_thenThrowBadRequestException() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(existingMember));
        StartMemberPaymentCommand command = new StartMemberPaymentCommand(
                MembershipType.MONTHLY, PaymentMethod.CASH, LocalDateTime.now().minusHours(1));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> paymentService.startForMember(command, socio));

        verifyNoInteractions(paymentRepository, appointmentRepository);
        assertEquals("La cita debe ser en una fecha y hora futura", exception.getMessage());
    }

    @Test
    void givenACashRenewalTooFarAhead_whenStartForMember_thenThrowBadRequestException() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(existingMember));
        StartMemberPaymentCommand command = new StartMemberPaymentCommand(
                MembershipType.MONTHLY, PaymentMethod.CASH, LocalDateTime.now().plusDays(31));

        assertThrows(BadRequestException.class, () -> paymentService.startForMember(command, socio));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void givenAnOpenPayment_whenStartForMember_thenThrowConflictException() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(existingMember));
        when(paymentRepository.existsByMemberIdAndStatusIn(1L, PaymentStatus.openStatuses())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> paymentService.startForMember(
                new StartMemberPaymentCommand(MembershipType.MONTHLY, PaymentMethod.SPEI, null), socio));

        verify(paymentRepository, never()).save(any());
        assertEquals("Ya tienes un pago en proceso. Termínalo o cancélalo antes de iniciar otro", exception.getMessage());
    }

    @Test
    void givenMercadoPagoIsNotConfigured_whenStartACardRenewal_thenThrowServiceUnavailableException() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(existingMember));
        when(paymentGatewayService.isConfigured()).thenReturn(false);

        assertThrows(ServiceUnavailableException.class, () -> paymentService.startForMember(
                new StartMemberPaymentCommand(MembershipType.MONTHLY, PaymentMethod.CARD, null), socio));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void givenAnAccountWithoutMember_whenStartForMember_thenThrowForbiddenException() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> paymentService.startForMember(
                new StartMemberPaymentCommand(MembershipType.MONTHLY, PaymentMethod.SPEI, null), socio));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void givenAPaymentOfAnotherMember_whenGetForMember_thenThrowNotFoundException() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(existingMember));
        PaymentEntity otherMembersPayment = PaymentEntity.builder()
                .id(20L)
                .memberId(2L)
                .membershipType(MembershipType.MONTHLY)
                .method(PaymentMethod.SPEI)
                .status(PaymentStatus.PENDING_BANK_VALIDATION)
                .amount(new BigDecimal("500.00"))
                .createdAt(Instant.now())
                .build();
        when(paymentRepository.findById(20L)).thenReturn(Optional.of(otherMembersPayment));

        OwnershipViolationException exception = assertThrows(OwnershipViolationException.class,
                () -> paymentService.getForMember(20L, socio));

        assertEquals("No se encontró el pago solicitado", exception.getMessage());
    }

    @Test
    void givenAPendingCashPayment_whenCancelForMember_thenCancelItAndItsAppointment() {
        PaymentEntity cashPayment = payment(10L, PaymentMethod.CASH, PaymentStatus.PENDING_APPOINTMENT);
        PaymentAppointmentEntity appointment = appointment(10L, AppointmentStatus.PENDING);
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(existingMember));
        when(paymentRepository.findWithLockById(10L)).thenReturn(Optional.of(cashPayment));
        when(appointmentRepository.findByPaymentIdAndStatusIn(any(), anyList())).thenReturn(List.of(appointment));
        when(appointmentRepository.findByPaymentIdInOrderByIdAsc(List.of(10L))).thenReturn(List.of(appointment));

        MemberPaymentView view = paymentService.cancelForMember(10L, socio);

        assertEquals(PaymentStatus.CANCELLED, view.payment().getStatus());
        assertEquals(AppointmentStatus.CANCELLED, view.appointment().getStatus());
        assertEquals("Cancelada por el socio", view.appointment().getCancellationReason());
    }

    @Test
    void givenAnApprovedPayment_whenCancelForMember_thenThrowBadRequestException() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(existingMember));
        when(paymentRepository.findWithLockById(10L))
                .thenReturn(Optional.of(payment(10L, PaymentMethod.SPEI, PaymentStatus.APPROVED)));

        assertThrows(BadRequestException.class, () -> paymentService.cancelForMember(10L, socio));

        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void givenAnAppointmentCancelledByReception_whenRescheduleForMember_thenRequestANewOne() {
        PaymentEntity cashPayment = payment(10L, PaymentMethod.CASH, PaymentStatus.APPOINTMENT_CANCELLED);
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(existingMember));
        when(paymentRepository.findWithLockById(10L)).thenReturn(Optional.of(cashPayment));
        when(appointmentRepository.findByPaymentIdAndStatusIn(any(), anyList())).thenReturn(List.of());
        when(appointmentRepository.save(any(PaymentAppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MemberPaymentView view = paymentService.rescheduleForMember(10L, tomorrowEvening.plusHours(1), socio);

        assertEquals(PaymentStatus.PENDING_APPOINTMENT, view.payment().getStatus());
        assertEquals(tomorrowEvening.plusHours(1), view.appointment().getScheduledAt());
        assertEquals(AppointmentStatus.PENDING, view.appointment().getStatus());
    }

    @Test
    void givenATransfer_whenRescheduleForMember_thenThrowBadRequestException() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(existingMember));
        when(paymentRepository.findWithLockById(10L))
                .thenReturn(Optional.of(payment(10L, PaymentMethod.SPEI, PaymentStatus.PENDING_BANK_VALIDATION)));

        assertThrows(BadRequestException.class, () -> paymentService.rescheduleForMember(10L, tomorrowEvening, socio));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void givenAnInvalidSignature_whenHandleGatewayNotification_thenThrowUnauthorizedWithoutAskingMercadoPago() {
        when(paymentGatewayService.isSignatureValid("123456", "request-1", "ts=1700000000,v1=abc")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> paymentService.handleGatewayNotification(SIGNED_NOTIFICATION));

        verify(paymentGatewayService, never()).fetchPayment(any());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void givenANotificationThatIsNotAboutAPayment_whenHandleGatewayNotification_thenIgnoreIt() {
        paymentService.handleGatewayNotification(new GatewayNotification("merchant_order", "1", "request-1", null));

        verifyNoInteractions(paymentGatewayService, paymentRepository, memberService);
    }

    @Test
    void givenAnApprovedChargeForTheRightAmount_whenHandleGatewayNotification_thenApproveAndRenew() {
        PaymentEntity cardPayment = onlineCardPayment(7L);
        givenASignedNotificationFor(new GatewayPayment("123456", "approved", "accredited", "7",
                new BigDecimal("500.0"), "MXN"));
        when(paymentRepository.findWithLockById(7L)).thenReturn(Optional.of(cardPayment));

        paymentService.handleGatewayNotification(SIGNED_NOTIFICATION);

        verify(memberService).renew(1L, MembershipType.MONTHLY);
        verify(eventPublisher).publishEvent(new MembershipRenewedEvent(1L));
        verify(auditService).record(new AuditEntry(null, null, "MercadoPago.paymentApproved", "POST",
                "/v1/payment-webhooks/mercado-pago", "7", AuditOutcome.SUCCESS, 200));
        assertEquals(PaymentStatus.APPROVED, cardPayment.getStatus());
        assertEquals("123456", cardPayment.getGatewayPaymentId());
    }

    @Test
    void givenAPaymentAlreadyApproved_whenTheSameNotificationArrivesAgain_thenDoNotRenewTwice() {
        PaymentEntity cardPayment = onlineCardPayment(7L);
        cardPayment.approveFromGateway(Instant.now(), "123456");
        givenASignedNotificationFor(new GatewayPayment("123456", "approved", "accredited", "7",
                new BigDecimal("500.00"), "MXN"));
        when(paymentRepository.findWithLockById(7L)).thenReturn(Optional.of(cardPayment));

        paymentService.handleGatewayNotification(SIGNED_NOTIFICATION);

        verifyNoInteractions(memberService, eventPublisher);
    }

    @Test
    void givenAChargeForADifferentAmount_whenHandleGatewayNotification_thenDoNotApprove() {
        PaymentEntity cardPayment = onlineCardPayment(7L);
        givenASignedNotificationFor(new GatewayPayment("123456", "approved", "accredited", "7",
                new BigDecimal("1.00"), "MXN"));
        when(paymentRepository.findWithLockById(7L)).thenReturn(Optional.of(cardPayment));

        paymentService.handleGatewayNotification(SIGNED_NOTIFICATION);

        verifyNoInteractions(memberService);
        assertEquals(PaymentStatus.AWAITING_GATEWAY, cardPayment.getStatus());
    }

    @Test
    void givenARejectedCharge_whenHandleGatewayNotification_thenMarkTheAttemptAsRejected() {
        PaymentEntity cardPayment = onlineCardPayment(7L);
        givenASignedNotificationFor(new GatewayPayment("123456", "rejected", "cc_rejected_insufficient_amount", "7",
                new BigDecimal("500.00"), "MXN"));
        when(paymentRepository.findWithLockById(7L)).thenReturn(Optional.of(cardPayment));

        paymentService.handleGatewayNotification(SIGNED_NOTIFICATION);

        verifyNoInteractions(memberService);
        assertEquals(PaymentStatus.GATEWAY_REJECTED, cardPayment.getStatus());
        assertEquals("cc_rejected_insufficient_amount", cardPayment.getRejectionReason());
    }

    @Test
    void givenAPaymentCancelledWhileTheMemberWasPaying_whenTheChargeIsApproved_thenStillApproveIt() {
        PaymentEntity cardPayment = onlineCardPayment(7L);
        cardPayment.cancel();
        givenASignedNotificationFor(new GatewayPayment("123456", "approved", "accredited", "7",
                new BigDecimal("500.00"), "MXN"));
        when(paymentRepository.findWithLockById(7L)).thenReturn(Optional.of(cardPayment));

        paymentService.handleGatewayNotification(SIGNED_NOTIFICATION);

        verify(memberService).renew(1L, MembershipType.MONTHLY);
        assertEquals(PaymentStatus.APPROVED, cardPayment.getStatus());
    }

    @Test
    void givenAReferenceToAPaymentMadeAtReception_whenHandleGatewayNotification_thenIgnoreIt() {
        givenASignedNotificationFor(new GatewayPayment("123456", "approved", "accredited", "10",
                new BigDecimal("500.00"), "MXN"));
        when(paymentRepository.findWithLockById(10L))
                .thenReturn(Optional.of(payment(10L, PaymentMethod.SPEI, PaymentStatus.PENDING_BANK_VALIDATION)));

        paymentService.handleGatewayNotification(SIGNED_NOTIFICATION);

        verifyNoInteractions(memberService, eventPublisher);
    }

    private void givenAMemberWithoutOpenPayments() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(existingMember));
        when(paymentRepository.existsByMemberIdAndStatusIn(1L, PaymentStatus.openStatuses())).thenReturn(false);
        when(checkInRepository.findByMemberId(1L)).thenReturn(List.of());
    }

    private void givenASignedNotificationFor(GatewayPayment gatewayPayment) {
        when(paymentGatewayService.isSignatureValid("123456", "request-1", "ts=1700000000,v1=abc")).thenReturn(true);
        when(paymentGatewayService.fetchPayment("123456")).thenReturn(gatewayPayment);
    }

    private PaymentEntity payment(Long id, PaymentMethod method, PaymentStatus status) {
        return PaymentEntity.builder()
                .id(id)
                .memberId(1L)
                .membershipType(MembershipType.MONTHLY)
                .method(method)
                .status(status)
                .amount(new BigDecimal("500.00"))
                .discountPercent(0)
                .createdAt(Instant.now())
                .build();
    }

    private PaymentEntity onlineCardPayment(Long id) {
        return PaymentEntity.builder()
                .id(id)
                .memberId(1L)
                .membershipType(MembershipType.MONTHLY)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.AWAITING_GATEWAY)
                .amount(new BigDecimal("500.00"))
                .discountPercent(0)
                .gatewayPreferenceId("pref-" + id)
                .createdAt(Instant.now())
                .build();
    }

    private PaymentAppointmentEntity appointment(Long paymentId, AppointmentStatus status) {
        return PaymentAppointmentEntity.builder()
                .id(30L)
                .paymentId(paymentId)
                .memberId(1L)
                .scheduledAt(tomorrowEvening)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    private PaymentEntity withId(PaymentEntity payment, Long id) {
        return PaymentEntity.builder()
                .id(id)
                .memberId(payment.getMemberId())
                .membershipType(payment.getMembershipType())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .discountPercent(payment.getDiscountPercent())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private List<CheckInEntity> buildCheckInStreak(int businessDays) {
        List<CheckInEntity> checkIns = new ArrayList<>();
        LocalDate cursor = LocalDate.now();
        int added = 0;
        while (added < businessDays) {
            if (cursor.getDayOfWeek().getValue() < 6) {
                checkIns.add(CheckInEntity.builder()
                        .id((long) added)
                        .memberId(1L)
                        .method(CheckInMethod.PIN)
                        .checkedInAt(cursor.atStartOfDay(ZoneId.systemDefault()).toInstant())
                        .build());
                added++;
            }
            cursor = cursor.minusDays(1);
        }
        return checkIns;
    }
}

package com.afcgym.platform.service.impl;

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
import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import com.afcgym.platform.persistance.repositories.ICheckInRepository;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IPaymentAppointmentRepository;
import com.afcgym.platform.persistance.repositories.IPaymentRepository;
import com.afcgym.platform.service.IAuditService;
import com.afcgym.platform.service.IMemberService;
import com.afcgym.platform.service.IPaymentGatewayService;
import com.afcgym.platform.service.IPaymentService;
import com.afcgym.platform.service.dto.AuditEntry;
import com.afcgym.platform.service.dto.BankTransferDetails;
import com.afcgym.platform.service.dto.GatewayCheckout;
import com.afcgym.platform.service.dto.GatewayNotification;
import com.afcgym.platform.service.dto.GatewayPayment;
import com.afcgym.platform.service.dto.MemberPaymentView;
import com.afcgym.platform.service.dto.MembershipRenewedEvent;
import com.afcgym.platform.service.dto.PaymentRejectedEvent;
import com.afcgym.platform.service.dto.RenewMembershipCommand;
import com.afcgym.platform.service.dto.Requester;
import com.afcgym.platform.service.dto.StartMemberPaymentCommand;
import com.afcgym.platform.utils.AttendanceStreakCalculator;
import com.afcgym.platform.utils.PaymentReferenceFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements IPaymentService {

    private static final long STREAK_DAYS_FOR_SMALL_DISCOUNT = 80;
    private static final long STREAK_DAYS_FOR_LARGE_DISCOUNT = 150;
    private static final int SMALL_DISCOUNT_PERCENT = 10;
    private static final int LARGE_DISCOUNT_PERCENT = 20;
    private static final String PAYMENT_NOTIFICATION_TYPE = "payment";
    private static final String GATEWAY_APPROVED = "approved";
    private static final Set<String> GATEWAY_FAILED = Set.of("rejected", "cancelled");
    private static final String CURRENCY = "MXN";
    private static final String CANCELLED_BY_MEMBER = "Cancelada por el socio";
    private static final String RESCHEDULED_BY_MEMBER = "Reagendada por el socio";
    private static final String PAYMENT_NOT_FOUND = "No se encontró el pago solicitado";
    private static final String GATEWAY_APPROVAL_ACTION = "MercadoPago.paymentApproved";
    private static final String GATEWAY_WEBHOOK_PATH = "/v1/payment-webhooks/mercado-pago";
    private static final List<AppointmentStatus> ACTIVE_APPOINTMENT_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.ACCEPTED);

    private final IPaymentRepository paymentRepository;
    private final IPaymentAppointmentRepository appointmentRepository;
    private final ICheckInRepository checkInRepository;
    private final IMemberRepository memberRepository;
    private final IMemberService memberService;
    private final IPaymentGatewayService paymentGatewayService;
    private final PaymentProperties paymentProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final IAuditService auditService;

    @Override
    @Transactional
    public PaymentEntity renew(Long memberId, RenewMembershipCommand command) {
        MemberEntity member = memberService.getById(memberId);
        validateMethodRequirements(command);

        int discountPercent = computeDiscountPercent(memberId);
        boolean approvedInstantly = command.method() == PaymentMethod.CARD;
        Instant now = Instant.now();

        PaymentEntity payment = paymentRepository.save(PaymentEntity.builder()
                .memberId(member.getId())
                .membershipType(command.membershipType())
                .method(command.method())
                .status(initialStatusAtReception(command.method()))
                .amount(priceFor(command.membershipType(), discountPercent))
                .discountPercent(discountPercent)
                .cardLastFour(command.cardLastFour())
                .scheduledDate(command.scheduledDate())
                .createdAt(now)
                .approvedAt(approvedInstantly ? now : null)
                .build());

        if (approvedInstantly) {
            renewMembership(payment);
        }
        return payment;
    }

    @Override
    public List<PaymentEntity> listByMember(Long memberId) {
        memberService.getById(memberId);
        return paymentRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Override
    @Transactional
    public PaymentEntity approve(Long paymentId, Long adminUserId) {
        PaymentEntity payment = lockPayment(paymentId);
        if (payment.isPaidThroughGateway()) {
            throw new BadRequestException("Los pagos con tarjeta en línea se confirman solos cuando Mercado Pago aprueba el cobro");
        }
        if (!payment.getStatus().isOpen()) {
            throw new BadRequestException("El pago ya fue procesado");
        }
        payment.approve(Instant.now(), adminUserId);
        renewMembership(payment);
        return payment;
    }

    @Override
    @Transactional
    public PaymentEntity reject(Long paymentId, String reason, Long adminUserId) {
        String normalizedReason = reason == null ? "" : reason.strip();
        if (normalizedReason.isEmpty()) {
            throw new BadRequestException("Escribe el motivo del rechazo");
        }
        PaymentEntity payment = lockPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING_BANK_VALIDATION) {
            throw new BadRequestException("Solo se pueden rechazar transferencias pendientes de validar");
        }
        payment.reject(Instant.now(), adminUserId, normalizedReason);
        eventPublisher.publishEvent(new PaymentRejectedEvent(payment.getMemberId(), normalizedReason));
        return payment;
    }

    @Override
    @Transactional
    public MemberPaymentView startForMember(StartMemberPaymentCommand command, Requester requester) {
        MemberEntity member = memberOf(requester);
        if (!member.isActive()) {
            throw new ForbiddenException("Tu cuenta de socio está dada de baja");
        }
        validateMemberMethod(command);
        if (paymentRepository.existsByMemberIdAndStatusIn(member.getId(), PaymentStatus.openStatuses())) {
            throw new ConflictException("Ya tienes un pago en proceso. Termínalo o cancélalo antes de iniciar otro");
        }

        int discountPercent = computeDiscountPercent(member.getId());
        PaymentEntity payment = paymentRepository.save(PaymentEntity.builder()
                .memberId(member.getId())
                .membershipType(command.membershipType())
                .method(command.method())
                .status(initialStatusForMember(command.method()))
                .amount(priceFor(command.membershipType(), discountPercent))
                .discountPercent(discountPercent)
                .createdAt(Instant.now())
                .build());

        PaymentAppointmentEntity appointment = null;
        if (command.method() == PaymentMethod.CARD) {
            GatewayCheckout checkout = paymentGatewayService.createCheckout(payment);
            payment.attachCheckout(checkout.preferenceId(), checkout.checkoutUrl());
        } else if (command.method() == PaymentMethod.CASH) {
            appointment = appointmentRepository.save(newAppointment(payment, command.appointmentAt()));
        }
        return view(payment, appointment);
    }

    @Override
    public List<MemberPaymentView> listForMember(Requester requester) {
        MemberEntity member = memberOf(requester);
        List<PaymentEntity> payments = paymentRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());
        if (payments.isEmpty()) {
            return List.of();
        }
        Map<Long, PaymentAppointmentEntity> latestAppointments = latestAppointments(
                payments.stream().map(PaymentEntity::getId).toList());
        return payments.stream()
                .map(payment -> view(payment, latestAppointments.get(payment.getId())))
                .toList();
    }

    @Override
    public MemberPaymentView getForMember(Long paymentId, Requester requester) {
        MemberEntity member = memberOf(requester);
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(PAYMENT_NOT_FOUND));
        ensureOwnedBy(payment, member);
        return view(payment, latestAppointments(List.of(paymentId)).get(paymentId));
    }

    @Override
    @Transactional
    public MemberPaymentView cancelForMember(Long paymentId, Requester requester) {
        MemberEntity member = memberOf(requester);
        PaymentEntity payment = lockOwnPayment(paymentId, member);
        if (!payment.getStatus().isOpen()) {
            throw new BadRequestException("Este pago ya no se puede cancelar");
        }
        payment.cancel();
        withdrawActiveAppointments(payment, CANCELLED_BY_MEMBER);
        return view(payment, latestAppointments(List.of(paymentId)).get(paymentId));
    }

    @Override
    @Transactional
    public MemberPaymentView rescheduleForMember(Long paymentId, LocalDateTime appointmentAt, Requester requester) {
        MemberEntity member = memberOf(requester);
        validateAppointmentTime(appointmentAt);
        PaymentEntity payment = lockOwnPayment(paymentId, member);
        if (payment.getMethod() != PaymentMethod.CASH || !payment.getStatus().isWaitingForAppointment()) {
            throw new BadRequestException("Solo se puede reagendar un pago en efectivo que siga pendiente");
        }
        withdrawActiveAppointments(payment, RESCHEDULED_BY_MEMBER);
        PaymentAppointmentEntity appointment = appointmentRepository.save(newAppointment(payment, appointmentAt));
        payment.awaitAppointmentConfirmation();
        return view(payment, appointment);
    }

    @Override
    @Transactional
    public void handleGatewayNotification(GatewayNotification notification) {
        if (!PAYMENT_NOTIFICATION_TYPE.equals(notification.type())) {
            return;
        }
        if (!paymentGatewayService.isSignatureValid(notification.dataId(), notification.requestId(),
                notification.signature())) {
            throw new UnauthorizedException("La firma de la notificación no es válida");
        }
        GatewayPayment gatewayPayment = paymentGatewayService.fetchPayment(notification.dataId());
        Optional<PaymentEntity> found = parsePaymentId(gatewayPayment.externalReference())
                .flatMap(paymentRepository::findWithLockById)
                .filter(PaymentEntity::isPaidThroughGateway);
        if (found.isEmpty()) {
            log.warn("Gateway notification for a payment that is not an online card payment of this gym");
            return;
        }
        PaymentEntity payment = found.get();
        if (payment.isApproved()) {
            return;
        }
        if (GATEWAY_APPROVED.equals(gatewayPayment.status())) {
            if (!matchesCharge(payment, gatewayPayment)) {
                log.warn("Gateway charge does not match payment {}", payment.getId());
                return;
            }
            payment.approveFromGateway(Instant.now(), gatewayPayment.id());
            renewMembership(payment);
            auditService.record(new AuditEntry(null, null, GATEWAY_APPROVAL_ACTION, "POST", GATEWAY_WEBHOOK_PATH,
                    String.valueOf(payment.getId()), AuditOutcome.SUCCESS, 200));
            return;
        }
        if (GATEWAY_FAILED.contains(gatewayPayment.status()) && payment.getStatus() == PaymentStatus.AWAITING_GATEWAY) {
            payment.markGatewayRejected(gatewayPayment.id(), gatewayPayment.statusDetail());
        }
    }

    private void renewMembership(PaymentEntity payment) {
        memberService.renew(payment.getMemberId(), payment.getMembershipType());
        eventPublisher.publishEvent(new MembershipRenewedEvent(payment.getMemberId()));
    }

    private PaymentStatus initialStatusAtReception(PaymentMethod method) {
        return switch (method) {
            case CARD -> PaymentStatus.APPROVED;
            case SPEI -> PaymentStatus.PENDING_BANK_VALIDATION;
            case CASH -> PaymentStatus.PENDING;
        };
    }

    private PaymentStatus initialStatusForMember(PaymentMethod method) {
        return switch (method) {
            case CARD -> PaymentStatus.AWAITING_GATEWAY;
            case SPEI -> PaymentStatus.PENDING_BANK_VALIDATION;
            case CASH -> PaymentStatus.PENDING_APPOINTMENT;
        };
    }

    private void validateMethodRequirements(RenewMembershipCommand command) {
        if (command.method() == PaymentMethod.CARD
                && (command.cardLastFour() == null || command.cardLastFour().isBlank())) {
            throw new BadRequestException("Se deben enviar los últimos 4 dígitos de la tarjeta");
        }
        if (command.method() == PaymentMethod.CASH && command.scheduledDate() == null) {
            throw new BadRequestException("Se debe enviar la fecha de la cita en recepción");
        }
    }

    private void validateMemberMethod(StartMemberPaymentCommand command) {
        switch (command.method()) {
            case CARD -> {
                if (!paymentGatewayService.isConfigured()) {
                    throw new ServiceUnavailableException("El pago con tarjeta no está disponible por ahora");
                }
            }
            case SPEI -> {
                if (!paymentProperties.bankTransfer().isConfigured()) {
                    throw new ServiceUnavailableException("El pago por transferencia no está disponible por ahora");
                }
            }
            case CASH -> validateAppointmentTime(command.appointmentAt());
        }
    }

    private void validateAppointmentTime(LocalDateTime appointmentAt) {
        if (appointmentAt == null) {
            throw new BadRequestException("Elige la fecha y hora en que pasarás a pagar a recepción");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!appointmentAt.isAfter(now)) {
            throw new BadRequestException("La cita debe ser en una fecha y hora futura");
        }
        int maxDaysAhead = paymentProperties.appointmentMaxDaysAhead();
        if (appointmentAt.isAfter(now.plusDays(maxDaysAhead))) {
            throw new BadRequestException("La cita no puede ser a más de " + maxDaysAhead + " días");
        }
    }

    private MemberEntity memberOf(Requester requester) {
        return memberRepository.findByUserId(requester.userId())
                .orElseThrow(() -> new ForbiddenException("Tu cuenta no está ligada a ningún socio"));
    }

    private PaymentEntity lockPayment(Long paymentId) {
        return paymentRepository.findWithLockById(paymentId)
                .orElseThrow(() -> new NotFoundException(PAYMENT_NOT_FOUND));
    }

    private PaymentEntity lockOwnPayment(Long paymentId, MemberEntity member) {
        PaymentEntity payment = lockPayment(paymentId);
        ensureOwnedBy(payment, member);
        return payment;
    }

    private void ensureOwnedBy(PaymentEntity payment, MemberEntity member) {
        if (!payment.getMemberId().equals(member.getId())) {
            throw new OwnershipViolationException(PAYMENT_NOT_FOUND);
        }
    }

    private PaymentAppointmentEntity newAppointment(PaymentEntity payment, LocalDateTime scheduledAt) {
        return PaymentAppointmentEntity.builder()
                .paymentId(payment.getId())
                .memberId(payment.getMemberId())
                .scheduledAt(scheduledAt)
                .status(AppointmentStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    private void withdrawActiveAppointments(PaymentEntity payment, String reason) {
        Instant now = Instant.now();
        appointmentRepository.findByPaymentIdAndStatusIn(payment.getId(), ACTIVE_APPOINTMENT_STATUSES)
                .forEach(appointment -> appointment.cancel(now, null, reason, null));
    }

    private Map<Long, PaymentAppointmentEntity> latestAppointments(List<Long> paymentIds) {
        return appointmentRepository.findByPaymentIdInOrderByIdAsc(paymentIds).stream()
                .collect(Collectors.toMap(PaymentAppointmentEntity::getPaymentId, appointment -> appointment,
                        (older, newer) -> newer));
    }

    private MemberPaymentView view(PaymentEntity payment, PaymentAppointmentEntity appointment) {
        PaymentProperties.BankTransfer bankTransfer = paymentProperties.bankTransfer();
        BankTransferDetails bankTransferDetails = payment.getStatus() == PaymentStatus.PENDING_BANK_VALIDATION
                && bankTransfer.isConfigured()
                ? new BankTransferDetails(bankTransfer.bankName(), bankTransfer.beneficiary(), bankTransfer.clabe(),
                        PaymentReferenceFormatter.reference(payment.getId()))
                : null;
        return new MemberPaymentView(payment, appointment, bankTransferDetails);
    }

    private Optional<Long> parsePaymentId(String externalReference) {
        if (externalReference == null || !externalReference.matches("\\d{1,18}")) {
            return Optional.empty();
        }
        return Optional.of(Long.valueOf(externalReference));
    }

    private boolean matchesCharge(PaymentEntity payment, GatewayPayment gatewayPayment) {
        return gatewayPayment.amount() != null
                && gatewayPayment.amount().compareTo(payment.getAmount()) == 0
                && CURRENCY.equals(gatewayPayment.currency());
    }

    private BigDecimal priceFor(MembershipType membershipType, int discountPercent) {
        return membershipType.getBasePrice()
                .multiply(BigDecimal.valueOf(100 - discountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private int computeDiscountPercent(Long memberId) {
        Set<LocalDate> attendanceDates = checkInRepository.findByMemberId(memberId).stream()
                .map(this::toLocalDate)
                .collect(Collectors.toSet());
        long streak = AttendanceStreakCalculator.countConsecutiveBusinessDays(attendanceDates, LocalDate.now());
        if (streak >= STREAK_DAYS_FOR_LARGE_DISCOUNT) {
            return LARGE_DISCOUNT_PERCENT;
        }
        if (streak >= STREAK_DAYS_FOR_SMALL_DISCOUNT) {
            return SMALL_DISCOUNT_PERCENT;
        }
        return 0;
    }

    private LocalDate toLocalDate(CheckInEntity checkIn) {
        return checkIn.getCheckedInAt().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}

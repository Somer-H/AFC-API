package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.CheckInEntity;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import com.afcgym.platform.persistance.repositories.ICheckInRepository;
import com.afcgym.platform.persistance.repositories.IPaymentRepository;
import com.afcgym.platform.service.IMemberService;
import com.afcgym.platform.service.IPaymentService;
import com.afcgym.platform.service.dto.RenewMembershipCommand;
import com.afcgym.platform.utils.AttendanceStreakCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements IPaymentService {

    private static final long STREAK_DAYS_FOR_SMALL_DISCOUNT = 80;
    private static final long STREAK_DAYS_FOR_LARGE_DISCOUNT = 150;
    private static final int SMALL_DISCOUNT_PERCENT = 10;
    private static final int LARGE_DISCOUNT_PERCENT = 20;

    private final IPaymentRepository paymentRepository;
    private final ICheckInRepository checkInRepository;
    private final IMemberService memberService;

    @Override
    @Transactional
    public PaymentEntity renew(Long memberId, RenewMembershipCommand command) {
        MemberEntity member = memberService.getById(memberId);
        validateMethodRequirements(command);

        int discountPercent = computeDiscountPercent(memberId);
        BigDecimal amount = command.membershipType().getBasePrice()
                .multiply(BigDecimal.valueOf(100 - discountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        boolean approvedInstantly = command.method() == PaymentMethod.CARD;
        Instant now = Instant.now();

        PaymentEntity payment = PaymentEntity.builder()
                .memberId(member.getId())
                .membershipType(command.membershipType())
                .method(command.method())
                .status(approvedInstantly ? PaymentStatus.APPROVED : PaymentStatus.PENDING)
                .amount(amount)
                .discountPercent(discountPercent)
                .cardLastFour(command.cardLastFour())
                .scheduledDate(command.scheduledDate())
                .createdAt(now)
                .approvedAt(approvedInstantly ? now : null)
                .build();
        payment = paymentRepository.save(payment);

        if (approvedInstantly) {
            memberService.renew(memberId, command.membershipType());
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
    public PaymentEntity approve(Long paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("No se encontró el pago solicitado"));
        if (!payment.isPending()) {
            throw new BadRequestException("El pago ya fue procesado");
        }
        payment.approve(Instant.now());
        memberService.renew(payment.getMemberId(), payment.getMembershipType());
        return payment;
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

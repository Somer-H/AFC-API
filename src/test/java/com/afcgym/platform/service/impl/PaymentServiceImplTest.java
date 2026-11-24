package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.CheckInEntity;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.persistance.entities.enums.CheckInMethod;
import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import com.afcgym.platform.persistance.repositories.ICheckInRepository;
import com.afcgym.platform.persistance.repositories.IPaymentRepository;
import com.afcgym.platform.service.IMemberService;
import com.afcgym.platform.service.dto.RenewMembershipCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private IPaymentRepository paymentRepository;

    @Mock
    private ICheckInRepository checkInRepository;

    @Mock
    private IMemberService memberService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private MemberEntity existingMember;

    @BeforeEach
    void init() {
        existingMember = MemberEntity.builder()
                .id(1L)
                .code("SOC-000001")
                .fullName("Ana Test")
                .membershipType(MembershipType.MONTHLY)
                .dueDate(LocalDate.now().plusDays(5))
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void givenACardPayment_whenRenew_thenApproveInstantlyAndRenewTheMembership() {
        when(memberService.getById(1L)).thenReturn(existingMember);
        when(checkInRepository.findByMemberId(1L)).thenReturn(List.of());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RenewMembershipCommand command = new RenewMembershipCommand(MembershipType.MONTHLY, PaymentMethod.CARD, "1234", null);

        PaymentEntity result = paymentService.renew(1L, command);

        assertEquals(PaymentStatus.APPROVED, result.getStatus());
        assertEquals(0, result.getDiscountPercent());
        verify(memberService).renew(1L, MembershipType.MONTHLY);
    }

    @Test
    void givenASpeiPayment_whenRenew_thenLeaveItPendingWithoutRenewing() {
        when(memberService.getById(1L)).thenReturn(existingMember);
        when(checkInRepository.findByMemberId(1L)).thenReturn(List.of());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RenewMembershipCommand command = new RenewMembershipCommand(MembershipType.MONTHLY, PaymentMethod.SPEI, null, null);

        PaymentEntity result = paymentService.renew(1L, command);

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(memberService, never()).renew(any(), any());
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
    }

    @Test
    void givenAnUnknownMember_whenListByMember_thenThrowNotFoundException() {
        when(memberService.getById(99L)).thenThrow(new NotFoundException("No se encontró el socio solicitado"));

        assertThrows(NotFoundException.class, () -> paymentService.listByMember(99L));
    }

    @Test
    void givenAPendingPayment_whenApprove_thenApproveAndRenewTheMembership() {
        PaymentEntity pendingPayment = PaymentEntity.builder()
                .id(10L)
                .memberId(1L)
                .membershipType(MembershipType.MONTHLY)
                .method(PaymentMethod.SPEI)
                .status(PaymentStatus.PENDING)
                .amount(BigDecimal.valueOf(500))
                .discountPercent(0)
                .createdAt(Instant.now())
                .build();
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(pendingPayment));

        PaymentEntity result = paymentService.approve(10L);

        assertEquals(PaymentStatus.APPROVED, result.getStatus());
        verify(memberService).renew(1L, MembershipType.MONTHLY);
    }

    @Test
    void givenAnAlreadyApprovedPayment_whenApprove_thenThrowBadRequestException() {
        PaymentEntity approvedPayment = PaymentEntity.builder()
                .id(11L)
                .memberId(1L)
                .membershipType(MembershipType.MONTHLY)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.APPROVED)
                .amount(BigDecimal.valueOf(500))
                .discountPercent(0)
                .createdAt(Instant.now())
                .approvedAt(Instant.now())
                .build();
        when(paymentRepository.findById(11L)).thenReturn(Optional.of(approvedPayment));

        assertThrows(BadRequestException.class, () -> paymentService.approve(11L));

        verify(memberService, never()).renew(any(), any());
    }

    @Test
    void givenAnUnknownPayment_whenApprove_thenThrowNotFoundException() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> paymentService.approve(99L));
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
            if (added < businessDays) {
                cursor = cursor.minusDays(1);
            }
        }
        return checkIns;
    }
}

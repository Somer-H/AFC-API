package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.ForbiddenException;
import com.afcgym.platform.persistance.entities.BookingEntity;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.ScheduleBlockEntity;
import com.afcgym.platform.persistance.entities.enums.BookingStatus;
import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.IBookingRepository;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IScheduleBlockRepository;
import com.afcgym.platform.service.dto.BookClassCommand;
import com.afcgym.platform.service.dto.Requester;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final Requester ADMIN = new Requester(1L, Role.ADMIN);
    private static final Requester SOCIO = new Requester(20L, Role.SOCIO);

    @Mock
    private IBookingRepository bookingRepository;

    @Mock
    private IScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private IMemberRepository memberRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private LocalDate nextWeek;
    private MemberEntity member;
    private ScheduleBlockEntity block;

    @BeforeEach
    void init() {
        nextWeek = LocalDate.now().plusDays(7);
        member = member(10L, LocalDate.now().plusDays(30));
        block = block(5L, nextWeek, true, 2);
    }

    @Test
    void givenASocioWithRoom_whenBook_thenSaveAnActiveBookingForTheirOwnMember() {
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(member));
        givenBlockWithBookings(0);
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingEntity result = bookingService.book(new BookClassCommand(5L, nextWeek, null), SOCIO);

        assertEquals(10L, result.getMemberId());
        assertEquals(nextWeek, result.getClassDate());
        assertEquals(BookingStatus.ACTIVE, result.getStatus());
        assertEquals(block, result.getScheduleBlock());
    }

    @Test
    void givenAnAdminWithoutMemberId_whenBook_thenThrowBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> bookingService.book(new BookClassCommand(5L, nextWeek, null), ADMIN));

        verifyNoInteractions(scheduleBlockRepository, bookingRepository);
    }

    @Test
    void givenAnAdminWithMemberId_whenBook_thenBookForThatMember() {
        when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
        givenBlockWithBookings(1);
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingEntity result = bookingService.book(new BookClassCommand(5L, nextWeek, 10L), ADMIN);

        assertEquals(10L, result.getMemberId());
    }

    @Test
    void givenASocioBookingForSomeoneElse_whenBook_thenThrowForbiddenException() {
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(member));

        assertThrows(ForbiddenException.class,
                () -> bookingService.book(new BookClassCommand(5L, nextWeek, 99L), SOCIO));
    }

    @Test
    void givenASocioAccountWithoutMember_whenBook_thenThrowForbiddenException() {
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> bookingService.book(new BookClassCommand(5L, nextWeek, null), SOCIO));
    }

    @Test
    void givenAnExpiredMembership_whenBook_thenThrowConflictException() {
        MemberEntity expired = member(10L, LocalDate.now().minusDays(3));
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(expired));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> bookingService.book(new BookClassCommand(5L, nextWeek, null), SOCIO));

        assertEquals("La membresía está vencida; renuévala para poder reservar", exception.getMessage());
        verifyNoInteractions(scheduleBlockRepository);
    }

    @Test
    void givenADeactivatedBlock_whenBook_thenThrowConflictException() {
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(member));
        when(scheduleBlockRepository.findWithLockById(5L)).thenReturn(Optional.of(block(5L, nextWeek, false, 2)));

        assertThrows(ConflictException.class,
                () -> bookingService.book(new BookClassCommand(5L, nextWeek, null), SOCIO));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void givenADateOnAnotherWeekday_whenBook_thenThrowBadRequestWithTheBlockDay() {
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(member));
        when(scheduleBlockRepository.findWithLockById(5L)).thenReturn(Optional.of(block));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> bookingService.book(new BookClassCommand(5L, nextWeek.plusDays(1), null), SOCIO));

        assertEquals(true, exception.getMessage().startsWith("Este horario es solo los "));
    }

    @Test
    void givenAPastDate_whenBook_thenThrowBadRequestException() {
        LocalDate lastWeek = LocalDate.now().minusDays(7);
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(member));
        when(scheduleBlockRepository.findWithLockById(5L)).thenReturn(Optional.of(block(5L, lastWeek, true, 2)));

        assertThrows(BadRequestException.class,
                () -> bookingService.book(new BookClassCommand(5L, lastWeek, null), SOCIO));
    }

    @Test
    void givenAnExistingBookingOfTheSameMember_whenBook_thenThrowConflictException() {
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(member));
        when(scheduleBlockRepository.findWithLockById(5L)).thenReturn(Optional.of(block));
        when(bookingRepository.existsByScheduleBlockIdAndMemberIdAndClassDateAndStatus(
                5L, 10L, nextWeek, BookingStatus.ACTIVE)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> bookingService.book(new BookClassCommand(5L, nextWeek, null), SOCIO));
    }

    @Test
    void givenAFullClass_whenBook_thenThrowConflictException() {
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(member));
        givenBlockWithBookings(2);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> bookingService.book(new BookClassCommand(5L, nextWeek, null), SOCIO));

        assertEquals("La clase ya alcanzó su cupo máximo", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void givenTheOwner_whenCancel_thenTheBookingBecomesCancelled() {
        BookingEntity booking = booking(10L, nextWeek, BookingStatus.ACTIVE);
        when(bookingRepository.findWithScheduleBlockById(3L)).thenReturn(Optional.of(booking));
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(member));

        BookingEntity result = bookingService.cancel(3L, SOCIO);

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
    }

    @Test
    void givenAnotherMembersBooking_whenASocioCancels_thenThrowForbiddenException() {
        when(bookingRepository.findWithScheduleBlockById(3L))
                .thenReturn(Optional.of(booking(99L, nextWeek, BookingStatus.ACTIVE)));
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(member));

        assertThrows(ForbiddenException.class, () -> bookingService.cancel(3L, SOCIO));
    }

    @Test
    void givenAnyBooking_whenAnAdminCancels_thenItIsCancelledWithoutCheckingOwnership() {
        when(bookingRepository.findWithScheduleBlockById(3L))
                .thenReturn(Optional.of(booking(99L, nextWeek, BookingStatus.ACTIVE)));

        BookingEntity result = bookingService.cancel(3L, ADMIN);

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verifyNoInteractions(memberRepository);
    }

    @Test
    void givenACancelledBooking_whenCancel_thenThrowConflictException() {
        when(bookingRepository.findWithScheduleBlockById(3L))
                .thenReturn(Optional.of(booking(10L, nextWeek, BookingStatus.CANCELLED)));

        assertThrows(ConflictException.class, () -> bookingService.cancel(3L, ADMIN));
    }

    @Test
    void givenAPastClass_whenCancel_thenThrowBadRequestException() {
        when(bookingRepository.findWithScheduleBlockById(3L))
                .thenReturn(Optional.of(booking(10L, LocalDate.now().minusDays(7), BookingStatus.ACTIVE)));

        assertThrows(BadRequestException.class, () -> bookingService.cancel(3L, ADMIN));
    }

    @Test
    void givenUpcomingBookings_whenListUpcoming_thenSortThemByDateAndStartTime() {
        BookingEntity later = booking(10L, nextWeek.plusDays(7), BookingStatus.ACTIVE);
        BookingEntity sooner = booking(10L, nextWeek, BookingStatus.ACTIVE);
        when(memberRepository.findByUserId(20L)).thenReturn(Optional.of(member));
        when(bookingRepository.findByMemberIdAndClassDateGreaterThanEqualAndStatus(
                10L, LocalDate.now(), BookingStatus.ACTIVE)).thenReturn(List.of(later, sooner));

        List<BookingEntity> result = bookingService.listUpcoming(SOCIO);

        assertEquals(List.of(sooner, later), result);
    }

    private void givenBlockWithBookings(long booked) {
        when(scheduleBlockRepository.findWithLockById(5L)).thenReturn(Optional.of(block));
        when(bookingRepository.existsByScheduleBlockIdAndMemberIdAndClassDateAndStatus(
                5L, 10L, nextWeek, BookingStatus.ACTIVE)).thenReturn(false);
        when(bookingRepository.countByScheduleBlockIdAndClassDateAndStatus(5L, nextWeek, BookingStatus.ACTIVE))
                .thenReturn(booked);
    }

    private MemberEntity member(Long id, LocalDate dueDate) {
        return MemberEntity.builder()
                .id(id)
                .code("SOC-" + id)
                .fullName("Ana Test")
                .membershipType(MembershipType.MONTHLY)
                .dueDate(dueDate)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    private ScheduleBlockEntity block(Long id, LocalDate classDate, boolean active, int maxCapacity) {
        return ScheduleBlockEntity.builder()
                .id(id)
                .dayOfWeek(classDate.getDayOfWeek())
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(8, 0))
                .maxCapacity(maxCapacity)
                .active(active)
                .createdAt(Instant.now())
                .build();
    }

    private BookingEntity booking(Long memberId, LocalDate classDate, BookingStatus status) {
        return BookingEntity.builder()
                .id(3L)
                .scheduleBlock(block(5L, classDate, true, 2))
                .memberId(memberId)
                .classDate(classDate)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }
}

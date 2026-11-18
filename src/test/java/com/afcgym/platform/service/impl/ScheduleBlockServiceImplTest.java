package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.BookingEntity;
import com.afcgym.platform.persistance.entities.CoachEntity;
import com.afcgym.platform.persistance.entities.ScheduleBlockEntity;
import com.afcgym.platform.persistance.entities.enums.BookingStatus;
import com.afcgym.platform.persistance.repositories.IBookingRepository;
import com.afcgym.platform.persistance.repositories.ICoachRepository;
import com.afcgym.platform.persistance.repositories.IScheduleBlockRepository;
import com.afcgym.platform.service.dto.BlockOccupancy;
import com.afcgym.platform.service.dto.DayOccupancy;
import com.afcgym.platform.service.dto.ScheduleBlockCommand;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleBlockServiceImplTest {

    private static final LocalTime SEVEN = LocalTime.of(7, 0);
    private static final LocalTime EIGHT = LocalTime.of(8, 0);

    @Mock
    private IScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private ICoachRepository coachRepository;

    @Mock
    private IBookingRepository bookingRepository;

    @InjectMocks
    private ScheduleBlockServiceImpl scheduleBlockService;

    @Test
    void givenAValidBlockWithCoach_whenCreate_thenSaveItActiveWithTheCoach() {
        CoachEntity coach = CoachEntity.builder().id(3L).name("Alex Rivera").createdAt(Instant.now()).build();
        when(coachRepository.findById(3L)).thenReturn(Optional.of(coach));
        when(scheduleBlockRepository.save(any(ScheduleBlockEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleBlockEntity result = scheduleBlockService.create(
                new ScheduleBlockCommand(DayOfWeek.MONDAY, SEVEN, EIGHT, 20, 3L));

        assertTrue(result.isActive());
        assertEquals(coach, result.getCoach());
        assertEquals(20, result.getMaxCapacity());
    }

    @Test
    void givenNoCoach_whenCreate_thenSaveItWithoutCoach() {
        when(scheduleBlockRepository.save(any(ScheduleBlockEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleBlockEntity result = scheduleBlockService.create(
                new ScheduleBlockCommand(DayOfWeek.MONDAY, SEVEN, EIGHT, 20, null));

        assertNull(result.getCoach());
    }

    @Test
    void givenAZeroCapacity_whenCreate_thenThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> scheduleBlockService.create(
                new ScheduleBlockCommand(DayOfWeek.MONDAY, SEVEN, EIGHT, 0, null)));

        verify(scheduleBlockRepository, never()).save(any());
    }

    @Test
    void givenAnEndBeforeTheStart_whenCreate_thenThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> scheduleBlockService.create(
                new ScheduleBlockCommand(DayOfWeek.MONDAY, EIGHT, SEVEN, 20, null)));
    }

    @Test
    void givenAnUnknownCoach_whenCreate_thenThrowNotFoundException() {
        when(coachRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> scheduleBlockService.create(
                new ScheduleBlockCommand(DayOfWeek.MONDAY, SEVEN, EIGHT, 20, 99L)));
    }

    @Test
    void givenUpcomingBookings_whenUpdateTheDay_thenThrowConflictException() {
        ScheduleBlockEntity block = block(1L, DayOfWeek.MONDAY, 20, true);
        when(scheduleBlockRepository.findWithCoachById(1L)).thenReturn(Optional.of(block));
        when(bookingRepository.findByScheduleBlockIdAndClassDateGreaterThanEqualAndStatus(
                1L, LocalDate.now(), BookingStatus.ACTIVE))
                .thenReturn(List.of(booking(block, LocalDate.now().plusDays(7))));

        assertThrows(ConflictException.class, () -> scheduleBlockService.update(
                1L, new ScheduleBlockCommand(DayOfWeek.TUESDAY, SEVEN, EIGHT, 20, null)));

        assertEquals(DayOfWeek.MONDAY, block.getDayOfWeek());
    }

    @Test
    void givenADateWithThreeBookings_whenLowerTheCapacityToTwo_thenThrowConflictException() {
        ScheduleBlockEntity block = block(1L, DayOfWeek.MONDAY, 20, true);
        LocalDate classDate = LocalDate.now().plusDays(7);
        when(scheduleBlockRepository.findWithCoachById(1L)).thenReturn(Optional.of(block));
        when(bookingRepository.findByScheduleBlockIdAndClassDateGreaterThanEqualAndStatus(
                1L, LocalDate.now(), BookingStatus.ACTIVE))
                .thenReturn(List.of(booking(block, classDate), booking(block, classDate), booking(block, classDate)));

        ConflictException exception = assertThrows(ConflictException.class, () -> scheduleBlockService.update(
                1L, new ScheduleBlockCommand(DayOfWeek.MONDAY, SEVEN, EIGHT, 2, null)));

        assertTrue(exception.getMessage().startsWith("Ya hay 3 reservas para el "));
        assertEquals(20, block.getMaxCapacity());
    }

    @Test
    void givenNoUpcomingBookings_whenUpdate_thenChangeEveryField() {
        ScheduleBlockEntity block = block(1L, DayOfWeek.MONDAY, 20, true);
        when(scheduleBlockRepository.findWithCoachById(1L)).thenReturn(Optional.of(block));
        when(bookingRepository.findByScheduleBlockIdAndClassDateGreaterThanEqualAndStatus(
                1L, LocalDate.now(), BookingStatus.ACTIVE)).thenReturn(List.of());

        ScheduleBlockEntity result = scheduleBlockService.update(
                1L, new ScheduleBlockCommand(DayOfWeek.FRIDAY, LocalTime.of(18, 0), LocalTime.of(19, 0), 12, null));

        assertEquals(DayOfWeek.FRIDAY, result.getDayOfWeek());
        assertEquals(LocalTime.of(18, 0), result.getStartTime());
        assertEquals(12, result.getMaxCapacity());
    }

    @Test
    void givenAnActiveBlock_whenDeactivate_thenItBecomesInactive() {
        ScheduleBlockEntity block = block(1L, DayOfWeek.MONDAY, 20, true);
        when(scheduleBlockRepository.findWithCoachById(1L)).thenReturn(Optional.of(block));

        assertFalse(scheduleBlockService.deactivate(1L).isActive());
    }

    @Test
    void givenAnInactiveBlock_whenActivate_thenItBecomesActive() {
        ScheduleBlockEntity block = block(1L, DayOfWeek.MONDAY, 20, false);
        when(scheduleBlockRepository.findWithCoachById(1L)).thenReturn(Optional.of(block));

        assertTrue(scheduleBlockService.activate(1L).isActive());
    }

    @Test
    void givenBookingsOnADate_whenOccupancy_thenCountOnlyThatDatesActiveBookingsPerBlock() {
        LocalDate date = LocalDate.now().plusDays(7);
        ScheduleBlockEntity morning = block(1L, date.getDayOfWeek(), 20, true);
        ScheduleBlockEntity evening = block(2L, date.getDayOfWeek(), 8, true);
        when(bookingRepository.findByClassDateAndStatus(date, BookingStatus.ACTIVE)).thenReturn(List.of(
                booking(morning, date), booking(morning, date), booking(morning, date),
                booking(evening, date), booking(evening, date)));
        when(scheduleBlockRepository.findByDayOfWeekOrderByStartTimeAsc(date.getDayOfWeek()))
                .thenReturn(List.of(morning, evening));

        DayOccupancy result = scheduleBlockService.occupancy(date, null);

        assertEquals(date, result.date());
        BlockOccupancy first = result.blocks().get(0);
        assertEquals(3, first.booked());
        assertEquals(17, first.available());
        assertEquals(0, new BigDecimal("15.00").compareTo(first.occupancyPercentage()));
        BlockOccupancy second = result.blocks().get(1);
        assertEquals(2, second.booked());
        assertEquals(0, new BigDecimal("25.00").compareTo(second.occupancyPercentage()));
    }

    @Test
    void givenOnlyADayOfWeek_whenOccupancy_thenUseItsNextOccurrenceIncludingToday() {
        LocalDate expected = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        when(bookingRepository.findByClassDateAndStatus(expected, BookingStatus.ACTIVE)).thenReturn(List.of());
        when(scheduleBlockRepository.findByDayOfWeekOrderByStartTimeAsc(DayOfWeek.WEDNESDAY)).thenReturn(List.of());

        DayOccupancy result = scheduleBlockService.occupancy(null, DayOfWeek.WEDNESDAY);

        assertEquals(expected, result.date());
    }

    @Test
    void givenADateThatIsNotTheGivenWeekday_whenOccupancy_thenThrowBadRequestException() {
        LocalDate monday = LocalDate.of(2026, 11, 16);

        assertThrows(BadRequestException.class, () -> scheduleBlockService.occupancy(monday, DayOfWeek.FRIDAY));
    }

    private ScheduleBlockEntity block(Long id, DayOfWeek dayOfWeek, int maxCapacity, boolean active) {
        return ScheduleBlockEntity.builder()
                .id(id)
                .dayOfWeek(dayOfWeek)
                .startTime(SEVEN)
                .endTime(EIGHT)
                .maxCapacity(maxCapacity)
                .active(active)
                .createdAt(Instant.now())
                .build();
    }

    private BookingEntity booking(ScheduleBlockEntity block, LocalDate classDate) {
        return BookingEntity.builder()
                .scheduleBlock(block)
                .memberId(1L)
                .classDate(classDate)
                .status(BookingStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }
}

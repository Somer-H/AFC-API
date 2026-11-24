package com.afcgym.platform.service.impl;

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
import com.afcgym.platform.service.IScheduleBlockService;
import com.afcgym.platform.service.dto.BlockOccupancy;
import com.afcgym.platform.service.dto.DayOccupancy;
import com.afcgym.platform.service.dto.ScheduleBlockCommand;
import com.afcgym.platform.utils.SpanishDateFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleBlockServiceImpl implements IScheduleBlockService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final IScheduleBlockRepository scheduleBlockRepository;
    private final ICoachRepository coachRepository;
    private final IBookingRepository bookingRepository;

    @Override
    @Transactional
    public ScheduleBlockEntity create(ScheduleBlockCommand command) {
        validate(command);
        CoachEntity coach = resolveCoach(command.coachId());
        return scheduleBlockRepository.save(ScheduleBlockEntity.builder()
                .dayOfWeek(command.dayOfWeek())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .maxCapacity(command.maxCapacity())
                .coach(coach)
                .active(true)
                .createdAt(Instant.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleBlockEntity getById(Long id) {
        return scheduleBlockRepository.findWithCoachById(id)
                .orElseThrow(() -> new NotFoundException("No se encontró el horario solicitado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleBlockEntity> list(DayOfWeek dayOfWeek) {
        if (dayOfWeek != null) {
            return scheduleBlockRepository.findByDayOfWeekOrderByStartTimeAsc(dayOfWeek);
        }
        return scheduleBlockRepository.findAll().stream()
                .sorted(Comparator.comparing(ScheduleBlockEntity::getDayOfWeek)
                        .thenComparing(ScheduleBlockEntity::getStartTime))
                .toList();
    }

    @Override
    @Transactional
    public ScheduleBlockEntity update(Long id, ScheduleBlockCommand command) {
        validate(command);
        ScheduleBlockEntity block = getById(id);
        CoachEntity coach = resolveCoach(command.coachId());
        List<BookingEntity> upcoming = bookingRepository.findByScheduleBlockIdAndClassDateGreaterThanEqualAndStatus(
                id, LocalDate.now(), BookingStatus.ACTIVE);
        if (!upcoming.isEmpty() && command.dayOfWeek() != block.getDayOfWeek()) {
            throw new ConflictException(
                    "El horario tiene reservas activas; cancélalas antes de cambiarlo de día");
        }
        Optional<Map.Entry<LocalDate, Long>> busiestDate = upcoming.stream()
                .collect(Collectors.groupingBy(BookingEntity::getClassDate, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue());
        if (busiestDate.isPresent() && busiestDate.get().getValue() > command.maxCapacity()) {
            throw new ConflictException("Ya hay " + busiestDate.get().getValue() + " reservas para el "
                    + SpanishDateFormatter.date(busiestDate.get().getKey()) + "; el cupo no puede ser menor");
        }
        block.updateDetails(
                command.dayOfWeek(), command.startTime(), command.endTime(), command.maxCapacity(), coach);
        return block;
    }

    @Override
    @Transactional
    public ScheduleBlockEntity activate(Long id) {
        ScheduleBlockEntity block = getById(id);
        block.activate();
        return block;
    }

    @Override
    @Transactional
    public ScheduleBlockEntity deactivate(Long id) {
        ScheduleBlockEntity block = getById(id);
        block.deactivate();
        return block;
    }

    @Override
    @Transactional(readOnly = true)
    public DayOccupancy occupancy(LocalDate date, DayOfWeek dayOfWeek) {
        if (date != null && dayOfWeek != null && date.getDayOfWeek() != dayOfWeek) {
            throw new BadRequestException("La fecha no corresponde al día de la semana indicado");
        }
        LocalDate targetDate = resolveDate(date, dayOfWeek);
        Map<Long, Long> bookedByBlock = bookingRepository.findByClassDateAndStatus(targetDate, BookingStatus.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(booking -> booking.getScheduleBlock().getId(), Collectors.counting()));
        List<BlockOccupancy> blocks = scheduleBlockRepository
                .findByDayOfWeekOrderByStartTimeAsc(targetDate.getDayOfWeek())
                .stream()
                .map(block -> toOccupancy(block, bookedByBlock.getOrDefault(block.getId(), 0L).intValue()))
                .toList();
        return new DayOccupancy(targetDate, targetDate.getDayOfWeek(), blocks);
    }

    private LocalDate resolveDate(LocalDate date, DayOfWeek dayOfWeek) {
        if (date != null) {
            return date;
        }
        LocalDate today = LocalDate.now();
        return dayOfWeek != null ? today.with(TemporalAdjusters.nextOrSame(dayOfWeek)) : today;
    }

    private BlockOccupancy toOccupancy(ScheduleBlockEntity block, int booked) {
        int available = Math.max(0, block.getMaxCapacity() - booked);
        BigDecimal percentage = BigDecimal.valueOf(booked)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(block.getMaxCapacity()), 2, RoundingMode.HALF_UP);
        return new BlockOccupancy(block, booked, available, percentage);
    }

    private void validate(ScheduleBlockCommand command) {
        if (command.maxCapacity() <= 0) {
            throw new BadRequestException("El cupo máximo debe ser mayor a cero");
        }
        if (!command.startTime().isBefore(command.endTime())) {
            throw new BadRequestException("La hora de inicio debe ser anterior a la hora de fin");
        }
    }

    private CoachEntity resolveCoach(Long coachId) {
        if (coachId == null) {
            return null;
        }
        return coachRepository.findById(coachId)
                .orElseThrow(() -> new NotFoundException("No se encontró el coach solicitado"));
    }
}

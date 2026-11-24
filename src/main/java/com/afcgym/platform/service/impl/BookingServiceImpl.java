package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.ForbiddenException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.BookingEntity;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.ScheduleBlockEntity;
import com.afcgym.platform.persistance.entities.enums.BookingStatus;
import com.afcgym.platform.persistance.entities.enums.MemberStatus;
import com.afcgym.platform.persistance.repositories.IBookingRepository;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IScheduleBlockRepository;
import com.afcgym.platform.service.IBookingService;
import com.afcgym.platform.service.dto.BookClassCommand;
import com.afcgym.platform.service.dto.Requester;
import com.afcgym.platform.utils.SpanishDateFormatter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements IBookingService {

    private final IBookingRepository bookingRepository;
    private final IScheduleBlockRepository scheduleBlockRepository;
    private final IMemberRepository memberRepository;

    @Override
    @Transactional
    public BookingEntity book(BookClassCommand command, Requester requester) {
        MemberEntity member = resolveMemberToBook(command.memberId(), requester);
        if (!member.isActive()) {
            throw new BadRequestException("El socio no está activo");
        }
        if (MemberStatus.fromDueDate(member.getDueDate()) != MemberStatus.ACTIVE) {
            throw new ConflictException("La membresía está vencida; renuévala para poder reservar");
        }

        ScheduleBlockEntity block = scheduleBlockRepository.findWithLockById(command.scheduleBlockId())
                .orElseThrow(() -> new NotFoundException("No se encontró el horario solicitado"));
        if (!block.isActive()) {
            throw new ConflictException("Este horario está desactivado y no acepta reservas");
        }
        LocalDate classDate = command.classDate();
        if (classDate.getDayOfWeek() != block.getDayOfWeek()) {
            throw new BadRequestException(
                    "Este horario es solo los " + SpanishDateFormatter.dayName(block.getDayOfWeek()));
        }
        if (hasStarted(block, classDate)) {
            throw new BadRequestException("No se puede reservar una clase que ya comenzó");
        }
        if (bookingRepository.existsByScheduleBlockIdAndMemberIdAndClassDateAndStatus(
                block.getId(), member.getId(), classDate, BookingStatus.ACTIVE)) {
            throw new ConflictException("El socio ya tiene una reserva en esta clase");
        }
        long booked = bookingRepository.countByScheduleBlockIdAndClassDateAndStatus(
                block.getId(), classDate, BookingStatus.ACTIVE);
        if (booked >= block.getMaxCapacity()) {
            throw new ConflictException("La clase ya alcanzó su cupo máximo");
        }

        return bookingRepository.save(BookingEntity.builder()
                .scheduleBlock(block)
                .memberId(member.getId())
                .classDate(classDate)
                .status(BookingStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
    }

    @Override
    @Transactional
    public BookingEntity cancel(Long id, Requester requester) {
        BookingEntity booking = bookingRepository.findWithScheduleBlockById(id)
                .orElseThrow(() -> new NotFoundException("No se encontró la reserva solicitada"));
        if (!requester.isAdmin() && !booking.getMemberId().equals(ownMember(requester).getId())) {
            throw new ForbiddenException("Solo puedes cancelar tus propias reservas");
        }
        if (!booking.isActive()) {
            throw new ConflictException("La reserva ya estaba cancelada");
        }
        if (hasStarted(booking.getScheduleBlock(), booking.getClassDate())) {
            throw new BadRequestException("Ya no se puede cancelar una clase que ya comenzó");
        }
        booking.cancel(Instant.now());
        return booking;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingEntity> listUpcoming(Requester requester) {
        MemberEntity member = ownMember(requester);
        return bookingRepository
                .findByMemberIdAndClassDateGreaterThanEqualAndStatus(member.getId(), LocalDate.now(), BookingStatus.ACTIVE)
                .stream()
                .sorted(Comparator.comparing(BookingEntity::getClassDate)
                        .thenComparing(booking -> booking.getScheduleBlock().getStartTime()))
                .toList();
    }

    private MemberEntity resolveMemberToBook(Long requestedMemberId, Requester requester) {
        if (requester.isAdmin()) {
            if (requestedMemberId == null) {
                throw new BadRequestException("Indica el socio para el que es la reserva");
            }
            return memberRepository.findById(requestedMemberId)
                    .orElseThrow(() -> new NotFoundException("No se encontró el socio solicitado"));
        }
        MemberEntity own = ownMember(requester);
        if (requestedMemberId != null && !requestedMemberId.equals(own.getId())) {
            throw new ForbiddenException("Solo puedes reservar para ti");
        }
        return own;
    }

    private MemberEntity ownMember(Requester requester) {
        return memberRepository.findByUserId(requester.userId())
                .orElseThrow(() -> new ForbiddenException("Tu cuenta no está ligada a ningún socio"));
    }

    private boolean hasStarted(ScheduleBlockEntity block, LocalDate classDate) {
        LocalDate today = LocalDate.now();
        if (classDate.isBefore(today)) {
            return true;
        }
        return classDate.isEqual(today) && !LocalTime.now().isBefore(block.getStartTime());
    }
}

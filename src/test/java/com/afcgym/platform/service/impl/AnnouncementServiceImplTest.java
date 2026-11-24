package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.AnnouncementDismissalEntity;
import com.afcgym.platform.persistance.entities.AnnouncementEntity;
import com.afcgym.platform.persistance.entities.enums.AnnouncementType;
import com.afcgym.platform.persistance.repositories.IAnnouncementDismissalRepository;
import com.afcgym.platform.persistance.repositories.IAnnouncementRepository;
import com.afcgym.platform.service.dto.PublishAnnouncementCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceImplTest {

    private static final String SESSION = "session-1";

    @Mock
    private IAnnouncementRepository announcementRepository;

    @Mock
    private IAnnouncementDismissalRepository dismissalRepository;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    @Test
    void givenAValidCommand_whenPublish_thenSaveItTrimmedWithItsAuthor() {
        LocalDate today = LocalDate.now();
        when(announcementRepository.save(any(AnnouncementEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnnouncementEntity result = announcementService.publish(new PublishAnnouncementCommand(
                "  Aviso  ", "  Mensaje  ", AnnouncementType.URGENT, today, today.plusDays(2), 7L));

        assertEquals("Aviso", result.getTitle());
        assertEquals("Mensaje", result.getMessage());
        assertEquals(AnnouncementType.URGENT, result.getType());
        assertEquals(7L, result.getCreatedBy());
    }

    @Test
    void givenAnEndBeforeTheStart_whenPublish_thenThrowBadRequestException() {
        LocalDate start = LocalDate.now().plusDays(5);

        assertThrows(BadRequestException.class, () -> announcementService.publish(new PublishAnnouncementCommand(
                "Aviso", "Mensaje", AnnouncementType.INFO, start, start.minusDays(1), 7L)));

        verify(announcementRepository, never()).save(any());
    }

    @Test
    void givenAnEndDateAlreadyPast_whenPublish_thenThrowBadRequestException() {
        LocalDate today = LocalDate.now();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> announcementService.publish(new PublishAnnouncementCommand(
                        "Aviso", "Mensaje", AnnouncementType.INFO, today.minusDays(3), today.minusDays(1), 7L)));

        assertEquals("La fecha final ya pasó; el anuncio nunca se mostraría", exception.getMessage());
    }

    @Test
    void givenCurrentAnnouncements_whenListCurrent_thenHideDismissedOnesAndPutUrgentFirst() {
        LocalDate today = LocalDate.now();
        AnnouncementEntity promotion = announcement(1L, AnnouncementType.PROMOTION, today);
        AnnouncementEntity olderUrgent = announcement(2L, AnnouncementType.URGENT, today.minusDays(3));
        AnnouncementEntity newerUrgent = announcement(3L, AnnouncementType.URGENT, today);
        AnnouncementEntity dismissed = announcement(4L, AnnouncementType.URGENT, today);
        when(dismissalRepository.findByTokenId(SESSION)).thenReturn(List.of(AnnouncementDismissalEntity.builder()
                .announcementId(4L).tokenId(SESSION).dismissedAt(Instant.now()).build()));
        when(announcementRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(promotion, olderUrgent, dismissed, newerUrgent));

        List<AnnouncementEntity> result = announcementService.listCurrent(SESSION);

        assertEquals(List.of(newerUrgent, olderUrgent, promotion), result);
    }

    @Test
    void givenANewDismissal_whenDismiss_thenSaveItForThatSession() {
        when(announcementRepository.existsById(1L)).thenReturn(true);
        when(dismissalRepository.existsByAnnouncementIdAndTokenId(1L, SESSION)).thenReturn(false);

        announcementService.dismiss(1L, SESSION);

        verify(dismissalRepository).save(any(AnnouncementDismissalEntity.class));
    }

    @Test
    void givenAnAlreadyDismissedAnnouncement_whenDismiss_thenDoNothing() {
        when(announcementRepository.existsById(1L)).thenReturn(true);
        when(dismissalRepository.existsByAnnouncementIdAndTokenId(1L, SESSION)).thenReturn(true);

        announcementService.dismiss(1L, SESSION);

        verify(dismissalRepository, never()).save(any());
    }

    @Test
    void givenAnUnknownAnnouncement_whenDismiss_thenThrowNotFoundException() {
        when(announcementRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> announcementService.dismiss(99L, SESSION));
    }

    @Test
    void givenAnUnknownAnnouncement_whenDelete_thenThrowNotFoundException() {
        when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> announcementService.delete(99L));
    }

    @Test
    void givenDifferentDates_whenIsCurrentOn_thenRespectTheInclusiveRange() {
        LocalDate start = LocalDate.of(2026, 11, 10);
        AnnouncementEntity ranged = AnnouncementEntity.builder()
                .startDate(start).endDate(start.plusDays(2)).build();
        AnnouncementEntity openEnded = AnnouncementEntity.builder().startDate(start).build();

        assertFalse(ranged.isCurrentOn(start.minusDays(1)));
        assertTrue(ranged.isCurrentOn(start));
        assertTrue(ranged.isCurrentOn(start.plusDays(2)));
        assertFalse(ranged.isCurrentOn(start.plusDays(3)));
        assertTrue(openEnded.isCurrentOn(start.plusYears(1)));
    }

    private AnnouncementEntity announcement(Long id, AnnouncementType type, LocalDate startDate) {
        return AnnouncementEntity.builder()
                .id(id)
                .title("Aviso " + id)
                .message("Mensaje")
                .type(type)
                .startDate(startDate)
                .createdAt(Instant.now())
                .build();
    }
}

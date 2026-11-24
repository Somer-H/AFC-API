package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.AnnouncementDismissalEntity;
import com.afcgym.platform.persistance.entities.AnnouncementEntity;
import com.afcgym.platform.persistance.repositories.IAnnouncementDismissalRepository;
import com.afcgym.platform.persistance.repositories.IAnnouncementRepository;
import com.afcgym.platform.persistance.repositories.specifications.AnnouncementSpecifications;
import com.afcgym.platform.service.IAnnouncementService;
import com.afcgym.platform.service.dto.PublishAnnouncementCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements IAnnouncementService {

    private static final String ANNOUNCEMENT_NOT_FOUND = "No se encontró el anuncio solicitado";
    private static final Comparator<AnnouncementEntity> MOST_IMPORTANT_FIRST =
            Comparator.comparing(AnnouncementEntity::getType)
                    .thenComparing(AnnouncementEntity::getStartDate, Comparator.reverseOrder())
                    .thenComparing(AnnouncementEntity::getId, Comparator.reverseOrder());

    private final IAnnouncementRepository announcementRepository;
    private final IAnnouncementDismissalRepository dismissalRepository;

    @Override
    @Transactional
    public AnnouncementEntity publish(PublishAnnouncementCommand command) {
        if (command.endDate() != null) {
            if (command.endDate().isBefore(command.startDate())) {
                throw new BadRequestException("La fecha final no puede ser anterior a la inicial");
            }
            if (command.endDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("La fecha final ya pasó; el anuncio nunca se mostraría");
            }
        }
        return announcementRepository.save(AnnouncementEntity.builder()
                .title(command.title().trim())
                .message(command.message().trim())
                .type(command.type())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .createdBy(command.createdBy())
                .createdAt(Instant.now())
                .build());
    }

    @Override
    public List<AnnouncementEntity> list() {
        return announcementRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate", "id"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementEntity> listCurrent(String sessionId) {
        Set<Long> dismissed = dismissalRepository.findByTokenId(sessionId).stream()
                .map(AnnouncementDismissalEntity::getAnnouncementId)
                .collect(Collectors.toSet());
        return announcementRepository.findAll(AnnouncementSpecifications.currentOn(LocalDate.now())).stream()
                .filter(announcement -> !dismissed.contains(announcement.getId()))
                .sorted(MOST_IMPORTANT_FIRST)
                .toList();
    }

    @Override
    @Transactional
    public void dismiss(Long id, String sessionId) {
        if (!announcementRepository.existsById(id)) {
            throw new NotFoundException(ANNOUNCEMENT_NOT_FOUND);
        }
        if (dismissalRepository.existsByAnnouncementIdAndTokenId(id, sessionId)) {
            return;
        }
        dismissalRepository.save(AnnouncementDismissalEntity.builder()
                .announcementId(id)
                .tokenId(sessionId)
                .dismissedAt(Instant.now())
                .build());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AnnouncementEntity announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ANNOUNCEMENT_NOT_FOUND));
        announcementRepository.delete(announcement);
    }
}

package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.persistance.entities.AuditLogEntity;
import com.afcgym.platform.persistance.entities.enums.AuditOutcome;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.IAuditLogRepository;
import com.afcgym.platform.service.dto.AuditEntry;
import com.afcgym.platform.service.dto.AuditLogFilter;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private IAuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    @Captor
    private ArgumentCaptor<AuditLogEntity> entityCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    void givenAnAdminChange_whenRecord_thenStoreWhoWhenAndWhat() {
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.record(new AuditEntry(5L, Role.ADMIN, "Payments.approve", "PATCH", "/v1/payments/9/approve",
                "9", AuditOutcome.SUCCESS, 200));

        verify(auditLogRepository).save(entityCaptor.capture());
        AuditLogEntity saved = entityCaptor.getValue();
        assertEquals(5L, saved.getActorUserId());
        assertEquals(Role.ADMIN, saved.getActorRole());
        assertEquals("Payments.approve", saved.getAction());
        assertEquals("/v1/payments/9/approve", saved.getPath());
        assertEquals("9", saved.getTargetId());
        assertEquals(AuditOutcome.SUCCESS, saved.getOutcome());
        assertEquals(200, saved.getResponseStatus());
        assertNotNull(saved.getOccurredAt());
    }

    @Test
    void givenAVeryLongPath_whenRecord_thenTruncateItToTheColumnSize() {
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.record(new AuditEntry(null, null, "AccessDenied", "GET", "/v1/" + "x".repeat(400), null,
                AuditOutcome.DENIED, 403));

        verify(auditLogRepository).save(entityCaptor.capture());
        assertEquals(300, entityCaptor.getValue().getPath().length());
    }

    @Test
    void givenFilters_whenSearch_thenAskForThatPageNewestFirst() {
        Page<AuditLogEntity> page = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AuditLogEntity> result = auditService.search(new AuditLogFilter(5L, "Payments.approve",
                AuditOutcome.SUCCESS, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)), 2, 20);

        verify(auditLogRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals(page, result);
        assertEquals(2, pageableCaptor.getValue().getPageNumber());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("occurredAt").getDirection());
    }

    @Test
    void givenATooBigPage_whenSearch_thenThrowBadRequestWithoutQuerying() {
        AuditLogFilter noFilter = new AuditLogFilter(null, null, null, null, null);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> auditService.search(noFilter, 0, 500));
        assertThrows(BadRequestException.class, () -> auditService.search(noFilter, -1, 20));

        verifyNoInteractions(auditLogRepository);
        assertEquals("El tamaño de página debe estar entre 1 y 100", exception.getMessage());
    }

    @Test
    void givenAnEndDateBeforeTheStart_whenSearch_thenThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> auditService.search(new AuditLogFilter(null, null, null,
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)), 0, 20));

        verifyNoInteractions(auditLogRepository);
    }
}

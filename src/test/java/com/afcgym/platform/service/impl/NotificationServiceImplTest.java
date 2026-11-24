package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ForbiddenException;
import com.afcgym.platform.persistance.entities.ExpiryReminderEntity;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.PushTokenEntity;
import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.IExpiryReminderRepository;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IPushTokenRepository;
import com.afcgym.platform.service.IPushDeliveryService;
import com.afcgym.platform.service.dto.MembershipRenewedEvent;
import com.afcgym.platform.service.dto.PushDeliveryReport;
import com.afcgym.platform.service.dto.PushMessage;
import com.afcgym.platform.service.dto.Requester;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final String PHONE_TOKEN = "ExponentPushToken[phone-123]";
    private static final String TABLET_TOKEN = "ExponentPushToken[tablet-456]";

    @Mock
    private IPushTokenRepository pushTokenRepository;

    @Mock
    private IExpiryReminderRepository expiryReminderRepository;

    @Mock
    private IMemberRepository memberRepository;

    @Mock
    private IPushDeliveryService pushDeliveryService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Captor
    private ArgumentCaptor<List<PushMessage>> messagesCaptor;

    @Captor
    private ArgumentCaptor<List<ExpiryReminderEntity>> remindersCaptor;

    private LocalDate today;

    private MemberEntity member;

    private Requester socio;

    @BeforeEach
    void init() {
        today = LocalDate.of(2026, 11, 24);
        member = createMemberDummy(1L, today.plusDays(3));
        socio = new Requester(10L, Role.SOCIO);
    }

    @Test
    void givenAValidToken_whenRegisterPushToken_thenSaveItForTheSignedInMember() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(member));
        when(pushTokenRepository.findByToken(PHONE_TOKEN)).thenReturn(Optional.empty());
        when(pushTokenRepository.save(any(PushTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PushTokenEntity result = notificationService.registerPushToken("  " + PHONE_TOKEN + " ", socio);

        verify(pushTokenRepository).save(any(PushTokenEntity.class));
        assertEquals(1L, result.getMemberId());
        assertEquals(PHONE_TOKEN, result.getToken());
    }

    @Test
    void givenATokenRegisteredByAnotherMember_whenRegisterPushToken_thenMoveItToTheSignedInMember() {
        PushTokenEntity sharedDevice = createPushTokenDummy(2L, PHONE_TOKEN);
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.of(member));
        when(pushTokenRepository.findByToken(PHONE_TOKEN)).thenReturn(Optional.of(sharedDevice));

        PushTokenEntity result = notificationService.registerPushToken(PHONE_TOKEN, socio);

        verify(pushTokenRepository, never()).save(any(PushTokenEntity.class));
        assertEquals(1L, result.getMemberId());
    }

    @Test
    void givenATokenThatIsNotAnExpoToken_whenRegisterPushToken_thenThrowBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> notificationService.registerPushToken("abc123", socio));

        verifyNoInteractions(memberRepository, pushTokenRepository);
        assertEquals("El token de notificaciones no es válido", exception.getMessage());
    }

    @Test
    void givenAnAccountWithoutMember_whenRegisterPushToken_thenThrowForbiddenException() {
        when(memberRepository.findByUserId(10L)).thenReturn(Optional.empty());

        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> notificationService.registerPushToken(PHONE_TOKEN, socio));

        verifyNoInteractions(pushTokenRepository);
        assertEquals("Tu cuenta no está ligada a ningún socio", exception.getMessage());
    }

    @Test
    void givenAMemberWithTwoDevicesExpiringInThreeDays_whenSendExpiryReminders_thenNotifyBothAndRecordTheReminder() {
        givenExpiringMembers(member);
        when(expiryReminderRepository.findByDueDateBetween(today, today.plusDays(3))).thenReturn(List.of());
        when(pushTokenRepository.findByMemberIdIn(List.of(1L))).thenReturn(List.of(
                createPushTokenDummy(1L, PHONE_TOKEN), createPushTokenDummy(1L, TABLET_TOKEN)));
        when(pushDeliveryService.deliver(anyList()))
                .thenReturn(new PushDeliveryReport(Set.of(PHONE_TOKEN, TABLET_TOKEN), Set.of()));

        int sent = notificationService.sendExpiryReminders(today, 3);

        verify(pushDeliveryService).deliver(messagesCaptor.capture());
        verify(expiryReminderRepository).saveAll(remindersCaptor.capture());
        assertEquals(1, sent);
        assertEquals(2, messagesCaptor.getValue().size());
        PushMessage message = messagesCaptor.getValue().getFirst();
        assertEquals("Tu membresía está por vencer", message.title());
        assertEquals("Tu membresía vence en 3 días (27/11/2026). Renuévala para seguir entrenando.", message.body());
        assertEquals("EXPIRY_REMINDER", message.data().get("type"));
        assertEquals(today.plusDays(3), remindersCaptor.getValue().getFirst().getDueDate());
    }

    @Test
    void givenMembershipsDueTodayAndTomorrow_whenSendExpiryReminders_thenSayHoyAndManana() {
        MemberEntity dueToday = createMemberDummy(2L, today);
        MemberEntity dueTomorrow = createMemberDummy(3L, today.plusDays(1));
        givenExpiringMembers(dueToday, dueTomorrow);
        when(expiryReminderRepository.findByDueDateBetween(today, today.plusDays(3))).thenReturn(List.of());
        when(pushTokenRepository.findByMemberIdIn(List.of(2L, 3L))).thenReturn(List.of(
                createPushTokenDummy(2L, PHONE_TOKEN), createPushTokenDummy(3L, TABLET_TOKEN)));
        when(pushDeliveryService.deliver(anyList()))
                .thenReturn(new PushDeliveryReport(Set.of(PHONE_TOKEN, TABLET_TOKEN), Set.of()));

        notificationService.sendExpiryReminders(today, 3);

        verify(pushDeliveryService).deliver(messagesCaptor.capture());
        assertEquals("Tu membresía vence hoy (24/11/2026). Renuévala para seguir entrenando.",
                messagesCaptor.getValue().get(0).body());
        assertEquals("Tu membresía vence mañana (25/11/2026). Renuévala para seguir entrenando.",
                messagesCaptor.getValue().get(1).body());
    }

    @Test
    void givenAReminderAlreadySentForThatDueDate_whenSendExpiryReminders_thenDoNotSendItAgain() {
        givenExpiringMembers(member);
        when(expiryReminderRepository.findByDueDateBetween(today, today.plusDays(3)))
                .thenReturn(List.of(createReminderDummy(1L, member.getDueDate())));

        int sent = notificationService.sendExpiryReminders(today, 3);

        verify(pushTokenRepository, never()).findByMemberIdIn(anyList());
        verifyNoInteractions(pushDeliveryService);
        assertEquals(0, sent);
    }

    @Test
    void givenAReminderForAPreviousDueDate_whenSendExpiryReminders_thenRemindAboutTheNewDueDate() {
        givenExpiringMembers(member);
        when(expiryReminderRepository.findByDueDateBetween(today, today.plusDays(3)))
                .thenReturn(List.of(createReminderDummy(1L, today.plusDays(1))));
        when(pushTokenRepository.findByMemberIdIn(List.of(1L))).thenReturn(List.of(createPushTokenDummy(1L, PHONE_TOKEN)));
        when(pushDeliveryService.deliver(anyList())).thenReturn(new PushDeliveryReport(Set.of(PHONE_TOKEN), Set.of()));

        int sent = notificationService.sendExpiryReminders(today, 3);

        assertEquals(1, sent);
    }

    @Test
    void givenNobodyExpiresWithinTheWindow_whenSendExpiryReminders_thenSendNothing() {
        givenExpiringMembers();

        int sent = notificationService.sendExpiryReminders(today, 3);

        verifyNoInteractions(expiryReminderRepository, pushTokenRepository, pushDeliveryService);
        assertEquals(0, sent);
    }

    @Test
    void givenAMemberWithoutDevices_whenSendExpiryReminders_thenSkipItWithoutRecordingAReminder() {
        givenExpiringMembers(member);
        when(expiryReminderRepository.findByDueDateBetween(today, today.plusDays(3))).thenReturn(List.of());
        when(pushTokenRepository.findByMemberIdIn(List.of(1L))).thenReturn(List.of());

        int sent = notificationService.sendExpiryReminders(today, 3);

        verifyNoInteractions(pushDeliveryService);
        verify(expiryReminderRepository, never()).saveAll(anyList());
        assertEquals(0, sent);
    }

    @Test
    void givenExpoAcceptsNoDevice_whenSendExpiryReminders_thenDoNotRecordItSoTheNextRunRetries() {
        givenExpiringMembers(member);
        when(expiryReminderRepository.findByDueDateBetween(today, today.plusDays(3))).thenReturn(List.of());
        when(pushTokenRepository.findByMemberIdIn(List.of(1L))).thenReturn(List.of(createPushTokenDummy(1L, PHONE_TOKEN)));
        when(pushDeliveryService.deliver(anyList())).thenReturn(new PushDeliveryReport(Set.of(), Set.of()));

        int sent = notificationService.sendExpiryReminders(today, 3);

        verify(expiryReminderRepository, never()).saveAll(anyList());
        assertEquals(0, sent);
    }

    @Test
    void givenADeviceThatIsNoLongerRegistered_whenSendExpiryReminders_thenDeleteItsToken() {
        givenExpiringMembers(member);
        when(expiryReminderRepository.findByDueDateBetween(today, today.plusDays(3))).thenReturn(List.of());
        when(pushTokenRepository.findByMemberIdIn(List.of(1L))).thenReturn(List.of(
                createPushTokenDummy(1L, PHONE_TOKEN), createPushTokenDummy(1L, TABLET_TOKEN)));
        when(pushDeliveryService.deliver(anyList()))
                .thenReturn(new PushDeliveryReport(Set.of(PHONE_TOKEN), Set.of(TABLET_TOKEN)));

        int sent = notificationService.sendExpiryReminders(today, 3);

        verify(pushTokenRepository).deleteByTokenIn(Set.of(TABLET_TOKEN));
        assertEquals(1, sent);
    }

    @Test
    void givenARenewedMembershipWithTwoDevices_whenNotifyMembershipRenewed_thenConfirmItOnBoth() {
        MemberEntity renewed = createMemberDummy(1L, today.plusDays(30));
        when(pushTokenRepository.findByMemberId(1L)).thenReturn(List.of(
                createPushTokenDummy(1L, PHONE_TOKEN), createPushTokenDummy(1L, TABLET_TOKEN)));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(renewed));
        when(pushDeliveryService.deliver(anyList()))
                .thenReturn(new PushDeliveryReport(Set.of(PHONE_TOKEN, TABLET_TOKEN), Set.of()));

        notificationService.notifyMembershipRenewed(new MembershipRenewedEvent(1L));

        verify(pushDeliveryService).deliver(messagesCaptor.capture());
        verify(pushTokenRepository, never()).deleteByTokenIn(any());
        assertEquals(2, messagesCaptor.getValue().size());
        PushMessage message = messagesCaptor.getValue().getFirst();
        assertEquals("Pago confirmado", message.title());
        assertEquals("Tu membresía quedó renovada. Vence el 24/12/2026.", message.body());
        assertEquals("MEMBERSHIP_RENEWED", message.data().get("type"));
    }

    @Test
    void givenARenewedMembershipWithoutDevices_whenNotifyMembershipRenewed_thenSendNothing() {
        when(pushTokenRepository.findByMemberId(1L)).thenReturn(List.of());

        notificationService.notifyMembershipRenewed(new MembershipRenewedEvent(1L));

        verify(memberRepository, never()).findById(any());
        verifyNoInteractions(pushDeliveryService);
    }

    private void givenExpiringMembers(MemberEntity... members) {
        when(memberRepository.findByActiveTrueAndDueDateBetweenOrderByDueDateAsc(today, today.plusDays(3)))
                .thenReturn(List.of(members));
    }

    private MemberEntity createMemberDummy(Long id, LocalDate dueDate) {
        return MemberEntity.builder()
                .id(id)
                .code("SOC-00000" + id)
                .fullName("Socio " + id)
                .membershipType(MembershipType.MONTHLY)
                .dueDate(dueDate)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    private PushTokenEntity createPushTokenDummy(Long memberId, String token) {
        return PushTokenEntity.builder()
                .memberId(memberId)
                .token(token)
                .registeredAt(Instant.now())
                .build();
    }

    private ExpiryReminderEntity createReminderDummy(Long memberId, LocalDate dueDate) {
        return ExpiryReminderEntity.builder()
                .memberId(memberId)
                .dueDate(dueDate)
                .sentAt(Instant.now())
                .build();
    }
}

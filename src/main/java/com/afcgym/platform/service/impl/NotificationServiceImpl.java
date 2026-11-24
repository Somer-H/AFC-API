package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ForbiddenException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.ExpiryReminderEntity;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.PushTokenEntity;
import com.afcgym.platform.persistance.repositories.IExpiryReminderRepository;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IPushTokenRepository;
import com.afcgym.platform.service.INotificationService;
import com.afcgym.platform.service.IPushDeliveryService;
import com.afcgym.platform.service.dto.MembershipRenewedEvent;
import com.afcgym.platform.service.dto.PushDeliveryReport;
import com.afcgym.platform.service.dto.PushMessage;
import com.afcgym.platform.service.dto.Requester;
import com.afcgym.platform.utils.SpanishDateFormatter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private static final Pattern EXPO_PUSH_TOKEN = Pattern.compile("^(ExponentPushToken|ExpoPushToken)\\[[^\\]\\s]+]$");
    private static final String DEFAULT_SOUND = "default";
    private static final String TYPE_KEY = "type";
    private static final String DUE_DATE_KEY = "dueDate";
    private static final String EXPIRY_REMINDER_TYPE = "EXPIRY_REMINDER";
    private static final String MEMBERSHIP_RENEWED_TYPE = "MEMBERSHIP_RENEWED";

    private final IPushTokenRepository pushTokenRepository;
    private final IExpiryReminderRepository expiryReminderRepository;
    private final IMemberRepository memberRepository;
    private final IPushDeliveryService pushDeliveryService;

    @Override
    @Transactional
    public PushTokenEntity registerPushToken(String token, Requester requester) {
        String normalizedToken = token == null ? "" : token.strip();
        if (!EXPO_PUSH_TOKEN.matcher(normalizedToken).matches()) {
            throw new BadRequestException("El token de notificaciones no es válido");
        }
        MemberEntity member = memberRepository.findByUserId(requester.userId())
                .orElseThrow(() -> new ForbiddenException("Tu cuenta no está ligada a ningún socio"));
        Instant now = Instant.now();
        Optional<PushTokenEntity> existing = pushTokenRepository.findByToken(normalizedToken);
        if (existing.isPresent()) {
            existing.get().assignTo(member.getId(), now);
            return existing.get();
        }
        return pushTokenRepository.save(PushTokenEntity.builder()
                .memberId(member.getId())
                .token(normalizedToken)
                .registeredAt(now)
                .build());
    }

    @Override
    @Transactional
    public int sendExpiryReminders(LocalDate today, int daysBefore) {
        LocalDate lastDay = today.plusDays(daysBefore);
        List<MemberEntity> expiring = memberRepository.findByActiveTrueAndDueDateBetweenOrderByDueDateAsc(today, lastDay);
        if (expiring.isEmpty()) {
            return 0;
        }
        List<ExpiryReminderEntity> alreadySent = expiryReminderRepository.findByDueDateBetween(today, lastDay);
        List<MemberEntity> pending = expiring.stream()
                .filter(member -> !wasReminded(member, alreadySent))
                .toList();
        if (pending.isEmpty()) {
            return 0;
        }
        Map<Long, List<PushTokenEntity>> tokensByMember = pushTokenRepository
                .findByMemberIdIn(pending.stream().map(MemberEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(PushTokenEntity::getMemberId));
        List<PushMessage> messages = new ArrayList<>();
        for (MemberEntity member : pending) {
            for (PushTokenEntity pushToken : tokensByMember.getOrDefault(member.getId(), List.of())) {
                messages.add(expiryReminderMessage(pushToken.getToken(), member.getDueDate(), today));
            }
        }
        if (messages.isEmpty()) {
            return 0;
        }
        PushDeliveryReport report = pushDeliveryService.deliver(messages);
        removeUnregisteredTokens(report);
        Instant now = Instant.now();
        List<ExpiryReminderEntity> reminders = pending.stream()
                .filter(member -> reachedAnyDevice(tokensByMember.getOrDefault(member.getId(), List.of()), report))
                .map(member -> ExpiryReminderEntity.builder()
                        .memberId(member.getId())
                        .dueDate(member.getDueDate())
                        .sentAt(now)
                        .build())
                .toList();
        if (!reminders.isEmpty()) {
            expiryReminderRepository.saveAll(reminders);
        }
        return reminders.size();
    }

    @Override
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyMembershipRenewed(MembershipRenewedEvent event) {
        List<PushTokenEntity> pushTokens = pushTokenRepository.findByMemberId(event.memberId());
        if (pushTokens.isEmpty()) {
            return;
        }
        MemberEntity member = memberRepository.findById(event.memberId())
                .orElseThrow(() -> new NotFoundException("No se encontró el socio solicitado"));
        List<PushMessage> messages = pushTokens.stream()
                .map(pushToken -> renewalMessage(pushToken.getToken(), member.getDueDate()))
                .toList();
        removeUnregisteredTokens(pushDeliveryService.deliver(messages));
    }

    private boolean wasReminded(MemberEntity member, List<ExpiryReminderEntity> alreadySent) {
        return alreadySent.stream().anyMatch(reminder -> reminder.getMemberId().equals(member.getId())
                && reminder.getDueDate().equals(member.getDueDate()));
    }

    private boolean reachedAnyDevice(List<PushTokenEntity> pushTokens, PushDeliveryReport report) {
        return pushTokens.stream().anyMatch(pushToken -> report.acceptedTokens().contains(pushToken.getToken()));
    }

    private void removeUnregisteredTokens(PushDeliveryReport report) {
        if (!report.unregisteredTokens().isEmpty()) {
            pushTokenRepository.deleteByTokenIn(report.unregisteredTokens());
        }
    }

    private PushMessage expiryReminderMessage(String token, LocalDate dueDate, LocalDate today) {
        String body = "Tu membresía vence " + describeDaysLeft(ChronoUnit.DAYS.between(today, dueDate))
                + " (" + SpanishDateFormatter.date(dueDate) + "). Renuévala para seguir entrenando.";
        return new PushMessage(token, "Tu membresía está por vencer", body, DEFAULT_SOUND,
                Map.of(TYPE_KEY, EXPIRY_REMINDER_TYPE, DUE_DATE_KEY, dueDate.toString()));
    }

    private PushMessage renewalMessage(String token, LocalDate dueDate) {
        String body = "Tu membresía quedó renovada. Vence el " + SpanishDateFormatter.date(dueDate) + ".";
        return new PushMessage(token, "Pago confirmado", body, DEFAULT_SOUND,
                Map.of(TYPE_KEY, MEMBERSHIP_RENEWED_TYPE, DUE_DATE_KEY, dueDate.toString()));
    }

    private String describeDaysLeft(long daysLeft) {
        if (daysLeft == 0) {
            return "hoy";
        }
        if (daysLeft == 1) {
            return "mañana";
        }
        return "en " + daysLeft + " días";
    }
}

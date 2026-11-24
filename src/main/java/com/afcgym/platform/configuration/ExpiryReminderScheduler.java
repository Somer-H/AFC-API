package com.afcgym.platform.configuration;

import com.afcgym.platform.service.INotificationService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryReminderScheduler {

    private final INotificationService notificationService;
    private final NotificationProperties properties;

    @Scheduled(cron = "${app.notifications.expiry-reminder-cron}")
    public void sendExpiryReminders() {
        int sent = notificationService.sendExpiryReminders(LocalDate.now(), properties.expiryReminderDaysBefore());
        log.info("Expiry reminders sent: {}", sent);
    }
}

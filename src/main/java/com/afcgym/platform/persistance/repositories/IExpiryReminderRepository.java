package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.ExpiryReminderEntity;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IExpiryReminderRepository extends JpaRepository<@NonNull ExpiryReminderEntity, @NonNull Long> {

    List<ExpiryReminderEntity> findByDueDateBetween(LocalDate from, LocalDate to);
}

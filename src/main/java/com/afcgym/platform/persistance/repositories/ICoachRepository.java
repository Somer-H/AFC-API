package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.CoachEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICoachRepository extends JpaRepository<@NonNull CoachEntity, @NonNull Long> {
}

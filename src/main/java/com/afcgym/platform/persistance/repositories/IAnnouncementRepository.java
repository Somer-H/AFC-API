package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.AnnouncementEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IAnnouncementRepository extends
        JpaRepository<@NonNull AnnouncementEntity, @NonNull Long>,
        JpaSpecificationExecutor<@NonNull AnnouncementEntity> {
}

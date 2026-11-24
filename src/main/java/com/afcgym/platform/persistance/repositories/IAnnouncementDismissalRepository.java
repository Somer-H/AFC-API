package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.AnnouncementDismissalEntity;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAnnouncementDismissalRepository
        extends JpaRepository<@NonNull AnnouncementDismissalEntity, @NonNull Long> {

    boolean existsByAnnouncementIdAndTokenId(Long announcementId, String tokenId);

    List<AnnouncementDismissalEntity> findByTokenId(String tokenId);
}

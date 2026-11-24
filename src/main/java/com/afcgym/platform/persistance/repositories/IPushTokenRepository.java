package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.PushTokenEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPushTokenRepository extends JpaRepository<@NonNull PushTokenEntity, @NonNull Long> {

    Optional<PushTokenEntity> findByToken(String token);

    List<PushTokenEntity> findByMemberId(Long memberId);

    List<PushTokenEntity> findByMemberIdIn(Collection<Long> memberIds);

    void deleteByTokenIn(Collection<String> tokens);
}

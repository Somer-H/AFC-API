package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.CheckInEntity;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICheckInRepository extends JpaRepository<@NonNull CheckInEntity, @NonNull Long> {

    List<CheckInEntity> findByMemberId(Long memberId);
}

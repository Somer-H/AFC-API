package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.MemberEntity;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IMemberRepository extends
        JpaRepository<@NonNull MemberEntity, @NonNull Long>,
        JpaSpecificationExecutor<@NonNull MemberEntity> {

    boolean existsByCode(String code);

    Optional<MemberEntity> findByUserId(Long userId);
}

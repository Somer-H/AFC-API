package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.PasswordResetTokenEntity;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPasswordResetTokenRepository extends JpaRepository<@NonNull PasswordResetTokenEntity, @NonNull Long> {

    Optional<PasswordResetTokenEntity> findByToken(String token);
}

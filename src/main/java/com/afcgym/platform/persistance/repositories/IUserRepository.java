package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.entities.enums.Role;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IUserRepository extends JpaRepository<@NonNull UserEntity, @NonNull Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRole(Role role);
}

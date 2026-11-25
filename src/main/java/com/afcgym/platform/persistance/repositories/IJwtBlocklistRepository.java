package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.JwtBlocklistEntity;
import java.util.Collection;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IJwtBlocklistRepository extends JpaRepository<@NonNull JwtBlocklistEntity, @NonNull String> {

    boolean existsByJtiIn(Collection<String> identifiers);
}

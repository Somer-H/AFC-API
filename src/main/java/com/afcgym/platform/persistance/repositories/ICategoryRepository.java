package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.CategoryEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICategoryRepository extends JpaRepository<@NonNull CategoryEntity, @NonNull Long> {

    boolean existsByName(String name);
}

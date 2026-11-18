package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.SupplierEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ISupplierRepository extends JpaRepository<@NonNull SupplierEntity, @NonNull Long> {
}

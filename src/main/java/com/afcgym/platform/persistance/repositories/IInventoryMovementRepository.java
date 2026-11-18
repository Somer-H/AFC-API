package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.InventoryMovementEntity;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IInventoryMovementRepository extends JpaRepository<@NonNull InventoryMovementEntity, @NonNull Long> {

    List<InventoryMovementEntity> findByProductIdOrderByCreatedAtDesc(Long productId);
}

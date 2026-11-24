package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.SaleEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ISaleRepository extends JpaRepository<@NonNull SaleEntity, @NonNull Long> {

    @EntityGraph(attributePaths = "items")
    List<SaleEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(Instant from, Instant to);

    @EntityGraph(attributePaths = "items")
    Optional<SaleEntity> findWithItemsById(Long id);

    List<SaleEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);
}

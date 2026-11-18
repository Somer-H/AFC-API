package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.ProductEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface IProductRepository extends
        JpaRepository<@NonNull ProductEntity, @NonNull Long>,
        JpaSpecificationExecutor<@NonNull ProductEntity> {

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductEntity> findWithLockById(Long id);
}

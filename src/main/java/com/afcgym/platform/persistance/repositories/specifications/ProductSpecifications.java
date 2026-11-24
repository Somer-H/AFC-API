package com.afcgym.platform.persistance.repositories.specifications;

import com.afcgym.platform.persistance.entities.ProductEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<@NonNull ProductEntity> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<@NonNull ProductEntity> searchMatches(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern));
    }

    public static Specification<@NonNull ProductEntity> active() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<@NonNull ProductEntity> lowStock() {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("stock"), root.get("minStock"));
    }
}

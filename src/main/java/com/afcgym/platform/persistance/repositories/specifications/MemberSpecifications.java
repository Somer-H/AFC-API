package com.afcgym.platform.persistance.repositories.specifications;

import com.afcgym.platform.persistance.entities.MemberEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public final class MemberSpecifications {

    private MemberSpecifications() {
    }

    public static Specification<@NonNull MemberEntity> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<@NonNull MemberEntity> searchMatches(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), pattern),
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern));
    }
}

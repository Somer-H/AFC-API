package com.afcgym.platform.persistance.repositories.specifications;

import com.afcgym.platform.persistance.entities.AnnouncementEntity;
import java.time.LocalDate;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public final class AnnouncementSpecifications {

    private AnnouncementSpecifications() {
    }

    public static Specification<@NonNull AnnouncementEntity> currentOn(LocalDate date) {
        return (root, query, cb) -> cb.and(
                cb.lessThanOrEqualTo(root.<LocalDate>get("startDate"), date),
                cb.or(
                        cb.isNull(root.get("endDate")),
                        cb.greaterThanOrEqualTo(root.<LocalDate>get("endDate"), date)));
    }
}

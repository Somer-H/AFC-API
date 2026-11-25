package com.afcgym.platform.persistance.repositories.specifications;

import com.afcgym.platform.persistance.entities.AuditLogEntity;
import com.afcgym.platform.persistance.entities.enums.AuditOutcome;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<@NonNull AuditLogEntity> all() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    public static Specification<@NonNull AuditLogEntity> actorEquals(Long actorUserId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("actorUserId"), actorUserId);
    }

    public static Specification<@NonNull AuditLogEntity> actionEquals(String action) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("action"), action);
    }

    public static Specification<@NonNull AuditLogEntity> outcomeEquals(AuditOutcome outcome) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("outcome"), outcome);
    }

    public static Specification<@NonNull AuditLogEntity> occurredFrom(Instant from) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    public static Specification<@NonNull AuditLogEntity> occurredBefore(Instant before) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThan(root.get("occurredAt"), before);
    }
}

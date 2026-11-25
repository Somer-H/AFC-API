ALTER TABLE app_user DROP CONSTRAINT ck_app_user_role;

ALTER TABLE app_user ADD CONSTRAINT ck_app_user_role CHECK (role IN ('ADMIN', 'SOCIO', 'COACH'));

ALTER TABLE audit_log DROP CONSTRAINT ck_audit_log_actor_role;

ALTER TABLE audit_log ADD CONSTRAINT ck_audit_log_actor_role
    CHECK (actor_role IS NULL OR actor_role IN ('ADMIN', 'SOCIO', 'COACH'));

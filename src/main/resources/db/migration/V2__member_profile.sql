ALTER TABLE member ADD COLUMN code VARCHAR(20);
ALTER TABLE member ADD COLUMN phone VARCHAR(20);
ALTER TABLE member ADD COLUMN email VARCHAR(120);
ALTER TABLE member ADD COLUMN membership_type VARCHAR(20);
ALTER TABLE member ADD COLUMN due_date DATE;

UPDATE member SET code = 'SOC-' || id, membership_type = 'MONTHLY', due_date = CURRENT_DATE WHERE code IS NULL;

ALTER TABLE member ALTER COLUMN code SET NOT NULL;
ALTER TABLE member ALTER COLUMN membership_type SET NOT NULL;
ALTER TABLE member ALTER COLUMN due_date SET NOT NULL;

ALTER TABLE member ADD CONSTRAINT uq_member_code UNIQUE (code);
ALTER TABLE member ADD CONSTRAINT ck_member_membership_type
    CHECK (membership_type IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'));

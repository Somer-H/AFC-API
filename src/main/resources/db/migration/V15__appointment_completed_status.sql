ALTER TABLE payment_appointment DROP CONSTRAINT ck_payment_appointment_status;

ALTER TABLE payment_appointment ADD CONSTRAINT ck_payment_appointment_status
    CHECK (status IN ('PENDING', 'ACCEPTED', 'CANCELLED', 'COMPLETED'));

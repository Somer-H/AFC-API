CREATE INDEX ix_member_due_date ON member (due_date);

CREATE INDEX ix_member_created_at ON member (created_at);

CREATE INDEX ix_check_in_member_checked_in_at ON check_in (member_id, checked_in_at);

CREATE INDEX ix_payment_status_approved_at ON payment (status, approved_at);

CREATE INDEX ix_payment_appointment_member_id ON payment_appointment (member_id);

CREATE INDEX ix_inventory_movement_product_created_at ON inventory_movement (product_id, created_at);

CREATE INDEX ix_product_category_id ON product (category_id);

CREATE INDEX ix_product_supplier_id ON product (supplier_id);

package com.afcgym.platform;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseSchemaTests {

    private static final List<String> EXPECTED_TABLES = List.of(
            "app_user", "member", "password_reset_token", "check_in", "jwt_blocklist", "payment",
            "category", "supplier", "product", "inventory_movement", "sale", "sale_item",
            "coach", "schedule_block", "booking", "routine", "exercise", "announcement",
            "announcement_dismissal", "push_token", "expiry_reminder", "payment_appointment");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void givenTheMigrations_whenTheApplicationStarts_thenAllTablesExist() {
        List<String> existingTables = jdbc.queryForList(
                "SELECT LOWER(table_name) FROM information_schema.tables WHERE LOWER(table_schema) = 'public'",
                String.class);
        for (String table : EXPECTED_TABLES) {
            assertTrue(existingTables.contains(table), "Missing table: " + table);
        }
    }

    @Test
    void givenAnInvalidRole_whenInsertingAUser_thenThrowDataIntegrityViolationException() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO app_user (username, full_name, email, password_hash, role) "
                        + "VALUES ('user1', 'User One', 'user1@afcgym.com', 'hash', 'OWNER')"));
    }

    @Test
    void givenAnInvalidMethod_whenInsertingACheckIn_thenThrowDataIntegrityViolationException() {
        long memberId = insertMember("Ana Test");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO check_in (member_id, method, checked_in_at) VALUES (?, 'QR', CURRENT_TIMESTAMP)",
                memberId));
    }

    @Test
    void givenADuplicateEmail_whenInsertingAUser_thenThrowDataIntegrityViolationException() {
        jdbc.update("INSERT INTO app_user (username, full_name, email, password_hash, role) "
                + "VALUES ('user2', 'User Two', 'duplicate@afcgym.com', 'hash', 'ADMIN')");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO app_user (username, full_name, email, password_hash, role) "
                        + "VALUES ('user3', 'User Three', 'duplicate@afcgym.com', 'hash', 'ADMIN')"));
    }

    @Test
    void givenAnInvalidMembershipType_whenInsertingAMember_thenThrowDataIntegrityViolationException() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO member (code, full_name, membership_type, due_date) "
                        + "VALUES ('SOC-999999', 'Invalid Member', 'DAILY', CURRENT_DATE)"));
    }

    @Test
    void givenADuplicateCode_whenInsertingAMember_thenThrowDataIntegrityViolationException() {
        insertMember("Ana Test", "SOC-DUP001");
        assertThrows(DataIntegrityViolationException.class, () -> insertMember("Another Member", "SOC-DUP001"));
    }

    @Test
    void givenAnInvalidStatus_whenInsertingAPayment_thenThrowDataIntegrityViolationException() {
        long memberId = insertMember("Ana Test");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO payment (member_id, membership_type, method, status, amount) "
                        + "VALUES (?, 'MONTHLY', 'CARD', 'REFUNDED', 500.00)",
                memberId));
    }

    @Test
    void givenAnInvalidType_whenInsertingAnInventoryMovement_thenThrowDataIntegrityViolationException() {
        jdbc.update("INSERT INTO product (code, name, price) VALUES ('SKU-SCHEMA', 'Producto', 100.00)");
        long productId = jdbc.queryForObject("SELECT id FROM product WHERE code = 'SKU-SCHEMA'", Long.class);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO inventory_movement (product_id, type, quantity, final_stock) VALUES (?, 'LOSS', 1, 0)",
                productId));
    }

    @Test
    void givenADuplicateProductCode_whenInserting_thenThrowDataIntegrityViolationException() {
        jdbc.update("INSERT INTO product (code, name, price) VALUES ('SKU-DUP', 'Producto', 100.00)");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO product (code, name, price) VALUES ('SKU-DUP', 'Otro', 50.00)"));
    }

    @Test
    void givenAnInvalidPaymentMethod_whenInsertingASale_thenThrowDataIntegrityViolationException() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO sale (folio, total, payment_method) VALUES ('V-SCHEMA', 100.00, 'BITCOIN')"));
    }

    @Test
    void givenAZeroQuantity_whenInsertingASaleItem_thenThrowDataIntegrityViolationException() {
        jdbc.update("INSERT INTO product (code, name, price) VALUES ('SKU-ITEM', 'Producto', 100.00)");
        long productId = jdbc.queryForObject("SELECT id FROM product WHERE code = 'SKU-ITEM'", Long.class);
        jdbc.update("INSERT INTO sale (folio, total, payment_method) VALUES ('V-ITEM', 0.00, 'CASH')");
        long saleId = jdbc.queryForObject("SELECT id FROM sale WHERE folio = 'V-ITEM'", Long.class);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO sale_item (sale_id, product_id, product_name, quantity, unit_price, subtotal) "
                        + "VALUES (?, ?, 'Producto', 0, 100.00, 0.00)",
                saleId, productId));
    }

    @Test
    void givenAZeroCapacity_whenInsertingAScheduleBlock_thenThrowDataIntegrityViolationException() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO schedule_block (day_of_week, start_time, end_time, max_capacity) "
                        + "VALUES ('MONDAY', '07:00', '08:00', 0)"));
    }

    @Test
    void givenAnEndBeforeTheStart_whenInsertingAScheduleBlock_thenThrowDataIntegrityViolationException() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO schedule_block (day_of_week, start_time, end_time, max_capacity) "
                        + "VALUES ('MONDAY', '09:00', '08:00', 10)"));
    }

    @Test
    void givenAnInvalidDay_whenInsertingAScheduleBlock_thenThrowDataIntegrityViolationException() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO schedule_block (day_of_week, start_time, end_time, max_capacity) "
                        + "VALUES ('LUNES', '07:00', '08:00', 10)"));
    }

    @Test
    void givenAnInvalidStatus_whenInsertingABooking_thenThrowDataIntegrityViolationException() {
        long memberId = insertMember("Ana Test");
        jdbc.update("INSERT INTO schedule_block (day_of_week, start_time, end_time, max_capacity) "
                + "VALUES ('MONDAY', '07:00', '08:00', 10)");
        long blockId = jdbc.queryForObject("SELECT MAX(id) FROM schedule_block", Long.class);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO booking (schedule_block_id, member_id, class_date, status) "
                        + "VALUES (?, ?, CURRENT_DATE, 'NO_SHOW')",
                blockId, memberId));
    }

    @Test
    void givenTwoMembersWithTheSameAccount_whenLinking_thenThrowDataIntegrityViolationException() {
        jdbc.update("INSERT INTO app_user (username, full_name, email, password_hash, role) "
                + "VALUES ('socio1', 'Socio Uno', 'socio1@afcgym.com', 'hash', 'SOCIO')");
        long userId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'socio1'", Long.class);
        long first = insertMember("Ana Test");
        long second = insertMember("Luis Test");
        jdbc.update("UPDATE member SET user_id = ? WHERE id = ?", userId, first);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE member SET user_id = ? WHERE id = ?", userId, second));
    }

    @Test
    void givenTwoRoutinesForTheSameDay_whenInserting_thenThrowDataIntegrityViolationException() {
        long coachId = insertCoach();
        jdbc.update("INSERT INTO routine (day_of_week, title, coach_id) VALUES ('MONDAY', 'Pierna', ?)", coachId);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO routine (day_of_week, title, coach_id) VALUES ('MONDAY', 'Brazo', ?)", coachId));
    }

    @Test
    void givenAnExerciseWithUrlAndFile_whenInserting_thenThrowDataIntegrityViolationException() {
        long coachId = insertCoach();
        jdbc.update("INSERT INTO routine (day_of_week, title, coach_id) VALUES ('FRIDAY', 'Pierna', ?)", coachId);
        long routineId = jdbc.queryForObject("SELECT id FROM routine WHERE day_of_week = 'FRIDAY'", Long.class);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO exercise (routine_id, sort_order, name, sets, reps, video_url, video_file) "
                        + "VALUES (?, 0, 'Sentadilla', 4, '10', 'https://youtu.be/a', 'abc.mp4')",
                routineId));
    }

    @Test
    void givenAnInvalidType_whenInsertingAnAnnouncement_thenThrowDataIntegrityViolationException() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO announcement (title, message, type, start_date) "
                        + "VALUES ('Aviso', 'Mensaje', 'ALERT', CURRENT_DATE)"));
    }

    @Test
    void givenAnEndBeforeTheStart_whenInsertingAnAnnouncement_thenThrowDataIntegrityViolationException() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO announcement (title, message, type, start_date, end_date) "
                        + "VALUES ('Aviso', 'Mensaje', 'INFO', DATE '2026-11-20', DATE '2026-11-19')"));
    }

    @Test
    void givenADismissedAnnouncement_whenDeletingIt_thenItsDismissalsAreDeletedToo() {
        jdbc.update("INSERT INTO announcement (title, message, type, start_date) "
                + "VALUES ('Cascada', 'Mensaje', 'INFO', CURRENT_DATE)");
        long announcementId = jdbc.queryForObject("SELECT id FROM announcement WHERE title = 'Cascada'", Long.class);
        jdbc.update("INSERT INTO announcement_dismissal (announcement_id, token_id) VALUES (?, 'session-1')",
                announcementId);

        jdbc.update("DELETE FROM announcement WHERE id = ?", announcementId);

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM announcement_dismissal WHERE announcement_id = ?", Integer.class, announcementId);
        assertTrue(remaining == 0);
    }

    @Test
    void givenATokenAlreadyRegistered_whenInsertingItAgain_thenThrowDataIntegrityViolationException() {
        long firstMember = insertMember("Ana Push");
        long secondMember = insertMember("Luis Push");
        jdbc.update("INSERT INTO push_token (member_id, token) VALUES (?, 'ExponentPushToken[same]')", firstMember);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO push_token (member_id, token) VALUES (?, 'ExponentPushToken[same]')", secondMember));
    }

    @Test
    void givenAReminderAlreadySent_whenInsertingTheSameDueDateAgain_thenThrowDataIntegrityViolationException() {
        long memberId = insertMember("Ana Recordatorio");
        jdbc.update("INSERT INTO expiry_reminder (member_id, due_date) VALUES (?, CURRENT_DATE)", memberId);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO expiry_reminder (member_id, due_date) VALUES (?, CURRENT_DATE)", memberId));
    }

    @Test
    void givenAnInvalidStatus_whenInsertingAPaymentAppointment_thenThrowDataIntegrityViolationException() {
        long memberId = insertMember("Ana Cita");
        jdbc.update("INSERT INTO payment (member_id, membership_type, method, status, amount) "
                + "VALUES (?, 'MONTHLY', 'CASH', 'PENDING_APPOINTMENT', 500)", memberId);
        long paymentId = jdbc.queryForObject("SELECT MAX(id) FROM payment", Long.class);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO payment_appointment (payment_id, member_id, scheduled_at, status) "
                        + "VALUES (?, ?, CURRENT_TIMESTAMP, 'MAYBE')", paymentId, memberId));
    }

    private long insertCoach() {
        jdbc.update("INSERT INTO coach (name) VALUES ('Coach Schema')");
        return jdbc.queryForObject("SELECT MAX(id) FROM coach", Long.class);
    }

    private long insertMember(String fullName) {
        return insertMember(fullName, "SOC-" + System.nanoTime());
    }

    private long insertMember(String fullName, String code) {
        jdbc.update("INSERT INTO member (code, full_name, membership_type, due_date, pin_hash) "
                + "VALUES (?, ?, 'MONTHLY', DATEADD('DAY', 30, CURRENT_DATE), 'hash')", code, fullName);
        return jdbc.queryForObject("SELECT MAX(id) FROM member", Long.class);
    }
}

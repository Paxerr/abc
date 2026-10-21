package com.danasea.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class Milestone1PostgreSqlEmpiricalChallengerTest {

    @Container
    static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withEnv("POSTGRES_DB", "challenger_test")
            .withEnv("POSTGRES_USER", "challenger_user")
            .withEnv("POSTGRES_PASSWORD", "challenger_password")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    private static void waitForPostgres(String url, String user, String password) throws Exception {
        long deadline = System.currentTimeMillis() + 60_000;
        Exception lastEx = null;
        while (System.currentTimeMillis() < deadline) {
            try (Connection conn = DriverManager.getConnection(url, user, password);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
                return;
            } catch (Exception e) {
                lastEx = e;
                Thread.sleep(250);
            }
        }
        throw new IllegalStateException("PostgreSQL did not become ready in time: " + (lastEx != null ? lastEx.getMessage() : "timeout"), lastEx);
    }

    @Test
    @DisplayName("Empirical Verification of V25 Migration Schema Changes on Real PostgreSQL 16")
    void verifyV25SchemaAndLargePayloadsOnPostgreSql16() throws Exception {
        String jdbcUrl = "jdbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getFirstMappedPort() + "/challenger_test";
        waitForPostgres(jdbcUrl, "challenger_user", "challenger_password");

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, "challenger_user", "challenger_password")
                .locations("classpath:db/migration")
                .connectRetries(30)
                .target("28")
                .load();

        var result = flyway.migrate();
        assertTrue(result.success);
        assertEquals("28", flyway.info().current().getVersion().getVersion());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, "challenger_user", "challenger_password");
             Statement stmt = conn.createStatement()) {

            // 1. Verify disputes new columns data types
            try (ResultSet rs = stmt.executeQuery("""
                    SELECT column_name, data_type, udt_name 
                    FROM information_schema.columns 
                    WHERE table_name = 'disputes' 
                      AND column_name IN ('vendor_response', 'vendor_evidence_urls', 'vendor_responded_at')
                    """)) {
                int found = 0;
                while (rs.next()) {
                    String col = rs.getString("column_name");
                    String type = rs.getString("data_type");
                    if ("vendor_response".equals(col)) {
                        assertEquals("text", type);
                        found++;
                    } else if ("vendor_evidence_urls".equals(col)) {
                        assertEquals("text", type);
                        found++;
                    } else if ("vendor_responded_at".equals(col)) {
                        assertTrue(type.contains("timestamp"));
                        found++;
                    }
                }
                assertEquals(3, found, "All 3 new vendor columns must exist in disputes table with correct data types");
            }

            // 2. Verify messages.content type is now text (relaxed from varchar(255))
            try (ResultSet rs = stmt.executeQuery("""
                    SELECT data_type 
                    FROM information_schema.columns 
                    WHERE table_name = 'messages' AND column_name = 'content'
                    """)) {
                assertTrue(rs.next());
                assertEquals("text", rs.getString("data_type"), "messages.content must be converted to text");
            }

            // 3. Verify new indexes on PostgreSQL
            try (ResultSet rs = stmt.executeQuery("""
                    SELECT indexname 
                    FROM pg_indexes 
                    WHERE schemaname = 'public' 
                      AND indexname IN ('idx_conversations_master_order_id', 'idx_conversations_customer_vendor', 'idx_messages_conversation_created')
                    """)) {
                int indexCount = 0;
                while (rs.next()) {
                    indexCount++;
                }
                assertEquals(3, indexCount, "All 3 V25 performance indexes must exist in pg_indexes");
            }

            // 4. Stress test: Insert message exceeding 255 chars (e.g., 2000 chars)
            UUID convId = UUID.randomUUID();
            UUID msgId = UUID.randomUUID();
            UUID senderId = UUID.randomUUID();
            String longMessage = "A".repeat(2000);

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO messages (id, conversation_id, sender_id, content, is_read, created_at, updated_at)
                    VALUES (?, ?, ?, ?, false, NOW(), NOW())
                    """)) {
                ps.setObject(1, msgId);
                ps.setObject(2, convId);
                ps.setObject(3, senderId);
                ps.setString(4, longMessage);
                int rows = ps.executeUpdate();
                assertEquals(1, rows);
            }

            // Read back long message
            try (ResultSet rs = stmt.executeQuery("SELECT content FROM messages WHERE id = '" + msgId + "'")) {
                assertTrue(rs.next());
                assertEquals(2000, rs.getString("content").length(), "Long message content must be persisted intact without truncation");
            }

            // 5. Insert dispute with vendor response, evidence URLs JSON, and vendor timestamp
            UUID disputeId = UUID.randomUUID();
            UUID subOrderId = UUID.randomUUID();
            UUID custId = UUID.randomUUID();

            // First insert dummy user for customer FK and dummy sub_order
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO users (id, email, password_hash, full_name, phone, role, created_at, updated_at)
                    VALUES (?, 'cust@test.com', 'hash', 'Customer Test', '0901234567', 'CUSTOMER', NOW(), NOW())
                    """)) {
                ps.setObject(1, custId);
                ps.executeUpdate();
            }

            // Create booking, service, sub_order for foreign key
            UUID serviceId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO services (id, name, price, status, created_at, updated_at)
                    VALUES (?, 'Service Test', 100000, 'PUBLISHED', NOW(), NOW())
                    """)) {
                ps.setObject(1, serviceId);
                ps.executeUpdate();
            }

            UUID bookingId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO bookings (id, customer_id, status, total_amount, created_at, updated_at)
                    VALUES (?, ?, 'CONFIRMED', 100000, NOW(), NOW())
                    """)) {
                ps.setObject(1, bookingId);
                ps.setObject(2, custId);
                ps.executeUpdate();
            }

            UUID bookingItemId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO booking_items (id, booking_id, service_id, vendor_id, quantity, booking_date, booking_time, price, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 1, DATE '2026-10-10', TIME '09:00:00', 100000, NOW(), NOW())
                    """)) {
                ps.setObject(1, bookingItemId);
                ps.setObject(2, bookingId);
                ps.setObject(3, serviceId);
                ps.setObject(4, custId);
                ps.executeUpdate();
            }

            UUID masterOrderId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO master_orders (id, customer_id, booking_id, status, total_amount, created_at, updated_at)
                    VALUES (?, ?, ?, 'PAID', 100000, NOW(), NOW())
                    """)) {
                ps.setObject(1, masterOrderId);
                ps.setObject(2, custId);
                ps.setObject(3, bookingId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO sub_orders (id, master_order_id, booking_item_id, status, created_at, updated_at)
                    VALUES (?, ?, ?, 'COMPLETED', NOW(), NOW())
                    """)) {
                ps.setObject(1, subOrderId);
                ps.setObject(2, masterOrderId);
                ps.setObject(3, bookingItemId);
                ps.executeUpdate();
            }

            // Now insert dispute with V25 fields
            String vendorResp = "Dịch vụ đã hoàn tất chuẩn chỉ theo hợp đồng.";
            String vendorEvidence = "[\"https://storage.danasea.com/evidence1.jpg\",\"https://storage.danasea.com/evidence2.jpg\"]";
            OffsetDateTime vendorTime = OffsetDateTime.now();

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO disputes (id, sub_order_id, customer_id, reason, description, status, vendor_response, vendor_evidence_urls, vendor_responded_at, created_at, updated_at)
                    VALUES (?, ?, ?, 'SAFETY_CONCERN', 'Customer claim', 'OPEN', ?, ?, ?, NOW(), NOW())
                    """)) {
                ps.setObject(1, disputeId);
                ps.setObject(2, subOrderId);
                ps.setObject(3, custId);
                ps.setString(4, vendorResp);
                ps.setString(5, vendorEvidence);
                ps.setObject(6, vendorTime);
                int count = ps.executeUpdate();
                assertEquals(1, count);
            }

            // Verify select from disputes
            try (ResultSet rs = stmt.executeQuery("SELECT vendor_response, vendor_evidence_urls, vendor_responded_at FROM disputes WHERE id = '" + disputeId + "'")) {
                assertTrue(rs.next());
                assertEquals(vendorResp, rs.getString("vendor_response"));
                assertEquals(vendorEvidence, rs.getString("vendor_evidence_urls"));
                assertNotNull(rs.getTimestamp("vendor_responded_at"));
            }
        }
    }
}

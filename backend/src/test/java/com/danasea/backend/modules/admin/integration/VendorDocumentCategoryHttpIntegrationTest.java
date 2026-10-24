package com.danasea.backend.modules.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.UserJpaEntity;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaUserRepository;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.vendor.domain.models.DocStatus;
import com.danasea.backend.modules.vendor.domain.models.DocType;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorDocumentJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorDocumentRepository;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;
import com.danasea.backend.security.authorization.BaseSecurityIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** Real HTTP/JWT + PostgreSQL; only disposable Testcontainers resources are used. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.autoconfigure.exclude=org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
        "logging.level.root=WARN", "logging.level.org.springframework=ERROR",
        "logging.level.com.danasea.backend=WARN",
        "spring.rabbitmq.host=127.0.0.1", "spring.rabbitmq.port=1",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false", "spring.rabbitmq.dynamic=false"
})
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VendorDocumentCategoryHttpIntegrationTest extends BaseSecurityIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired JpaUserRepository users;
    @Autowired JpaVendorRepository vendors;
    @Autowired JpaVendorDocumentRepository documents;
    @Autowired JpaServiceRepository services;
    @Autowired PasswordEncoder encoder;

    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    final Path evidence = Path.of("target/vendor-document-category-http-evidence.jsonl");
    UUID adminId;
    String adminToken;
    String vendorToken;
    String customerToken;
    UUID vendorId;
    UUID licenseId;
    UUID safetyId;
    UUID categoryId;

    @BeforeAll void authenticateOnIsolatedDatabase() throws Exception {
        // 1. Verify isolated test database (never target production 'danasea' database)
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).isEqualTo("testdb");
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class)).isEqualTo("public");

        // 2. Verify migration V37 applied successfully
        Integer v37Count = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '37' AND success = true", Integer.class);
        assertThat(v37Count).isEqualTo(1);

        // 3. Verify latest migration is at least V37 and no migrations failed (compatible with V37 and V38)
        Integer latestVersion = jdbc.queryForObject(
                "SELECT max(version::integer) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(latestVersion).isGreaterThanOrEqualTo(37);
        Integer failedCount = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false", Integer.class);
        assertThat(failedCount).isZero();

        // 4. Verify required schema for both VendorDocument and Category features exists
        Integer categoryColumns = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'categories'
                  AND column_name IN ('is_active', 'requires_safety_cert')
                """, Integer.class);
        assertThat(categoryColumns).isEqualTo(2);

        Integer documentColumns = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'vendor_documents'
                  AND column_name IN ('status', 'file_url', 'doc_type', 'vendor_id')
                """, Integer.class);
        assertThat(documentColumns).isEqualTo(4);

        Integer auditColumns = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'audit_logs'
                  AND column_name IN ('action', 'actor_user_id', 'entity_id', 'entity_type', 'metadata')
                """, Integer.class);
        assertThat(auditColumns).isEqualTo(5);

        Files.createDirectories(evidence.getParent());
        Files.writeString(evidence, "");
        // Other suites may enable framework DEBUG globally; never log login DTOs or token responses.
        LoggingSystem.get(getClass().getClassLoader()).setLogLevel("org.springframework.web", LogLevel.WARN);
        Files.writeString(evidence, json.writeValueAsString(Map.of("environment", Map.of(
                "httpBase", base(), "database", "testdb", "schema", "public", "flyway", latestVersion,
                "postgresql", jdbc.queryForObject("SELECT version()", String.class),
                "isolated", true))) + "\n", StandardOpenOption.APPEND);
        var admin = user(Role.ADMIN);
        adminId = admin.getId();
        adminToken = login(admin);
        vendorToken = login(user(Role.VENDOR));
        customerToken = login(user(Role.CUSTOMER));
    }

    UserJpaEntity user(Role role) {
        var user = new UserJpaEntity();
        user.setEmail(UUID.randomUUID() + "@review.test");
        user.setFullName("Isolated review fixture");
        user.setRole(role);
        user.setIsEmailVerified(true);
        user.setIsLocked(false);
        // Random credentials stay in memory; no credentials/tokens are written to evidence.
        user.setPasswordHash(encoder.encode("unused-" + UUID.randomUUID()));
        return users.saveAndFlush(user);
    }

    String login(UserJpaEntity user) throws Exception {
        String password = UUID.randomUUID().toString();
        user.setPasswordHash(encoder.encode(password));
        users.saveAndFlush(user);
        var response = http.send(HttpRequest.newBuilder(URI.create(base() + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(
                        Map.of("email", user.getEmail(), "password", password)))).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode result = json.readTree(response.body());
        String token = result.path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    @BeforeEach void fixtures() {
        var vendor = new VendorJpaEntity();
        vendor.setUserId(user(Role.VENDOR).getId());
        vendor.setBusinessName("Isolated document review");
        vendor.setVerificationStatus(VerificationStatus.PENDING);
        vendorId = vendors.saveAndFlush(vendor).getId();
        licenseId = document(DocType.BUSINESS_LICENSE, DocStatus.PENDING);
        safetyId = document(DocType.SAFETY_CERT, DocStatus.PENDING);
        categoryId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO categories(id,name,name_en,slug,icon_url,is_active,requires_safety_cert,created_at,updated_at)
                VALUES (?, 'Review category', 'Category English', ?, 'fixture-icon', false, true, now(), now())
                """, categoryId, "review-" + categoryId);
    }

    UUID document(DocType type, DocStatus status) {
        var document = new VendorDocumentJpaEntity();
        document.setVendorId(vendorId);
        document.setDocType(type);
        document.setStatus(status);
        document.setFileUrl("https://example.test/isolated-review-fixture.png");
        return documents.saveAndFlush(document).getId();
    }

    @Test void categoryActivatesDeactivatesAndReactivatesWithoutLosingOtherFields() throws Exception {
        var original = categorySnapshot();
        for (boolean active : List.of(true, false, true)) {
            var response = patch(categoryPath(), "{\"isActive\":" + active + "}", adminToken, 200);
            assertThat(response.path("isActive").asBoolean()).isEqualTo(active);
            var stored = categorySnapshot();
            assertThat(stored.get("is_active")).isEqualTo(active);
            assertUpdatedAt(response, stored);
            assertThat(((Timestamp) stored.get("updated_at")).toInstant())
                    .isAfter(((Timestamp) original.get("updated_at")).toInstant());
            stored.remove("is_active");
            stored.remove("updated_at");
            var expected = new LinkedHashMap<>(original);
            expected.remove("is_active");
            expected.remove("updated_at");
            assertThat(stored).isEqualTo(expected);
        }
    }

    @Test void categoryRenameAndAbsentOrNullActivationPreserveSafetyFlag() throws Exception {
        patch(categoryPath(), "{\"name\":\"Renamed\"}", adminToken, 200);
        assertThat(categorySnapshot()).containsEntry("name", "Renamed")
                .containsEntry("is_active", false).containsEntry("requires_safety_cert", true);
        patch(categoryPath(), "{\"isActive\":true}", adminToken, 200);
        var beforeNull = categorySnapshot();
        var response = patch(categoryPath(), "{\"isActive\":null}", adminToken, 200);
        assertThat(categorySnapshot()).isEqualTo(beforeNull);
        assertUpdatedAt(response, beforeNull);
    }

    @Test void categoryMissingAndUnauthorizedChangesAreRejected() throws Exception {
        patch("/api/admin/categories/" + UUID.randomUUID(), "{\"isActive\":true}", adminToken, 404);
        for (String token : List.of("", vendorToken, customerToken)) {
            patch(categoryPath(), "{\"isActive\":true}", token, token.isEmpty() ? 401 : 403);
        }
        assertThat(categorySnapshot().get("is_active")).isEqualTo(false);
    }

    @Test void patchCannotBypassPublishedServiceDeactivationGuard() throws Exception {
        patch(categoryPath(), "{\"isActive\":true}", adminToken, 200);
        var service = new ServiceJpaEntity();
        service.setName("Isolated published service");
        service.setCategoryId(categoryId);
        service.setVendorId(vendorId);
        service.setStatus(ServiceStatus.PUBLISHED);
        services.saveAndFlush(service);
        patch(categoryPath(), "{\"name\":\"Must rollback\",\"isActive\":false}", adminToken, 409);
        patch(categoryPath() + "/deactivate", "", adminToken, 409);
        assertThat(categorySnapshot()).containsEntry("name", "Review category").containsEntry("is_active", true);
    }

    @Test void existingDeactivateEndpointPreservesSafetyAndAllowsReactivation() throws Exception {
        patch(categoryPath(), "{\"isActive\":true}", adminToken, 200);
        patch(categoryPath() + "/deactivate", "", adminToken, 200);
        assertThat(categorySnapshot()).containsEntry("is_active", false).containsEntry("requires_safety_cert", true);
        patch(categoryPath(), "{\"isActive\":true}", adminToken, 200);
    }

    @Test void documentApprovePersistsActorTimeAndRejectsDuplicateOrOppositeDecision() throws Exception {
        var original = documentSnapshot(licenseId);
        var response = patch(documentPath(licenseId) + "/approve", "", adminToken, 200);
        var approved = documentSnapshot(licenseId);
        assertUpdatedAt(response, approved);
        assertThat(((Timestamp) approved.get("updated_at")).toInstant())
                .isAfter(((Timestamp) original.get("updated_at")).toInstant());
        assertThat(approved).containsEntry("status", "APPROVED").containsEntry("reviewed_by", adminId);
        assertThat(approved.get("reviewed_at")).isNotNull();
        for (String field : List.of("id", "vendor_id", "doc_type", "file_url", "created_at")) {
            assertThat(approved.get(field)).isEqualTo(original.get(field));
        }
        patch(documentPath(licenseId) + "/approve", "", adminToken, 409);
        patch(documentPath(licenseId) + "/reject", "{\"reason\":\"Already reviewed\"}", adminToken, 409);
        assertThat(documentSnapshot(licenseId)).isEqualTo(approved);
        assertThat(documentSnapshot(safetyId).get("status")).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT verification_status FROM vendors WHERE id=?", String.class, vendorId))
                .isEqualTo("PENDING");
        assertThat(auditCount(licenseId)).isEqualTo(1);
    }

    @Test void documentRejectPersistsReasonAtomicallyInAuditAndCannotBeReReviewed() throws Exception {
        var response = patch(documentPath(licenseId) + "/reject", "{\"reason\":\"  Expired evidence  \"}", adminToken, 200);
        assertUpdatedAt(response, documentSnapshot(licenseId));
        assertThat(documentSnapshot(licenseId)).containsEntry("status", "REJECTED").containsEntry("reviewed_by", adminId);
        assertThat(documentSnapshot(licenseId).get("reviewed_at")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT metadata FROM audit_logs WHERE entity_id=? AND action='VENDOR_DOCUMENT_REJECTED'",
                String.class, licenseId)).contains("Expired evidence");
        UUID auditId = jdbc.queryForObject("SELECT id FROM audit_logs WHERE entity_id=? AND action='VENDOR_DOCUMENT_REJECTED'",
                UUID.class, licenseId);
        String auditPath = "/api/admin/audit-logs/" + auditId;
        var auditResponse = get(auditPath, adminToken, 200);
        assertThat(auditResponse.path("entityId").asText()).isEqualTo(licenseId.toString());
        assertThat(auditResponse.path("metadata").asText()).contains("Rejection reason: Expired evidence");
        assertThat(get("/api/admin/audit-logs?size=100", adminToken, 200).findValuesAsText("metadata"))
                .anySatisfy(value -> assertThat(value).contains("Rejection reason: Expired evidence"));
        get(auditPath, "", 401);
        get(auditPath, vendorToken, 403);
        get(auditPath, customerToken, 403);
        patch(documentPath(licenseId) + "/reject", "{\"reason\":\"Overwrite\"}", adminToken, 409);
        patch(documentPath(licenseId) + "/approve", "", adminToken, 409);
        assertThat(auditCount(licenseId)).isEqualTo(1);
    }

    @Test void documentMissingWrongVendorAndOrphanAreNotReviewable() throws Exception {
        patch(documentPath(UUID.randomUUID()) + "/approve", "", adminToken, 404);
        patch("/api/admin/vendors/" + UUID.randomUUID() + "/documents/" + licenseId + "/approve", "", adminToken, 404);
        UUID otherVendor = vendorId;
        fixtures();
        patch("/api/admin/vendors/" + otherVendor + "/documents/" + licenseId + "/approve", "", adminToken, 404);
        assertThat(documentSnapshot(licenseId).get("status")).isEqualTo("PENDING");
        var orphan = new VendorDocumentJpaEntity();
        orphan.setVendorId(UUID.randomUUID());
        orphan.setDocType(DocType.BUSINESS_LICENSE);
        orphan.setStatus(DocStatus.PENDING);
        documents.saveAndFlush(orphan);
        patch("/api/admin/vendors/" + orphan.getVendorId() + "/documents/" + orphan.getId() + "/approve", "", adminToken, 404);
        assertThat(documentSnapshot(orphan.getId()).get("status")).isEqualTo("PENDING");
    }

    @Test void documentNullStateIsInvalid() throws Exception {
        UUID invalid = document(DocType.SAFETY_CERT, null);
        patch(documentPath(invalid) + "/approve", "", adminToken, 409);
        assertThat(documentSnapshot(invalid).get("status")).isNull();
    }

    @Test void documentRejectRequiresNonBlankBoundedReason() throws Exception {
        for (String body : List.of("{}", "{\"reason\":\"  \"}", "{\"reason\":\"" + "x".repeat(2001) + "\"}")) {
            patch(documentPath(licenseId) + "/reject", body, adminToken, 400);
        }
        assertThat(documentSnapshot(licenseId).get("status")).isEqualTo("PENDING");
        assertThat(auditCount(licenseId)).isZero();
    }

    @Test void documentReviewRequiresAdminForBothDecisions() throws Exception {
        for (String action : List.of("approve", "reject")) {
            for (String token : List.of("", vendorToken, customerToken)) {
                patch(documentPath(licenseId) + "/" + action, "{\"reason\":\"Not authorized\"}",
                        token, token.isEmpty() ? 401 : 403);
            }
        }
        assertThat(documentSnapshot(licenseId).get("status")).isEqualTo("PENDING");
        assertThat(auditCount(licenseId)).isZero();
    }

    @Test void vendorApprovalStillRequiresBothDocumentTypesApproved() throws Exception {
        String path = "/api/admin/vendors/" + vendorId + "/approve";
        patch(path, "", adminToken, 422);
        patch(documentPath(licenseId) + "/approve", "", adminToken, 200);
        patch(path, "", adminToken, 422);
        patch(documentPath(safetyId) + "/approve", "", adminToken, 200);
        patch(path, "", adminToken, 200);
        assertThat(jdbc.queryForObject("SELECT verification_status FROM vendors WHERE id=?", String.class, vendorId))
                .isEqualTo("APPROVED");
    }

    @Test void rejectedDocumentDoesNotSatisfyVendorApprovalGate() throws Exception {
        patch(documentPath(licenseId) + "/approve", "", adminToken, 200);
        patch(documentPath(safetyId) + "/reject", "{\"reason\":\"Insufficient evidence\"}", adminToken, 200);
        patch("/api/admin/vendors/" + vendorId + "/approve", "", adminToken, 422);
    }

    @Test void competingReviewsCommitOnlyOneDecisionAndOneAudit() throws Exception {
        var before = sqlSnapshot();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var approve = pool.submit(() -> { ready.countDown(); start.await(); return send(documentPath(licenseId) + "/approve", "", adminToken); });
            var reject = pool.submit(() -> { ready.countDown(); start.await(); return send(documentPath(licenseId) + "/reject", "{\"reason\":\"Concurrent review\"}", adminToken); });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            var a = approve.get(20, TimeUnit.SECONDS); var r = reject.get(20, TimeUnit.SECONDS);
            assertThat(List.of(a.statusCode(), r.statusCode())).containsExactlyInAnyOrder(200, 409);
            assertThat(documentSnapshot(licenseId).get("status")).isEqualTo(a.statusCode() == 200 ? "APPROVED" : "REJECTED");
            assertThat(auditCount(licenseId)).isEqualTo(1);
            record(documentPath(licenseId) + "/approve", "", adminToken, a, before);
            record(documentPath(licenseId) + "/reject", "{\"reason\":\"Concurrent review\"}", adminToken, r, before);
        }
    }

    @Test void auditPersistenceFailureRollsBackDocumentReview() throws Exception {
        var before = documentSnapshot(licenseId);
        // Deliberate storage failure in this disposable DB; no SQL fakes a successful review.
        jdbc.execute("ALTER TABLE audit_logs ADD CONSTRAINT review_test_failure CHECK(action <> 'VENDOR_DOCUMENT_REJECTED')");
        try {
            patch(documentPath(licenseId) + "/reject", "{\"reason\":\"Rollback probe\"}", adminToken, 500);
            assertThat(documentSnapshot(licenseId)).isEqualTo(before);
            assertThat(auditCount(licenseId)).isZero();
        } finally {
            jdbc.execute("ALTER TABLE audit_logs DROP CONSTRAINT review_test_failure");
        }
    }

    String base() { return "http://127.0.0.1:" + port; }
    String categoryPath() { return "/api/admin/categories/" + categoryId; }
    String documentPath(UUID id) { return "/api/admin/vendors/" + vendorId + "/documents/" + id; }
    Map<String,Object> categorySnapshot() {
        return jdbc.queryForMap("SELECT id,name,name_en,slug,parent_id,icon_url,is_active,requires_safety_cert,created_at,updated_at FROM categories WHERE id=?", categoryId);
    }
    Map<String,Object> documentSnapshot(UUID id) {
        return jdbc.queryForMap("SELECT id,vendor_id,doc_type,file_url,status,reviewed_by,reviewed_at,created_at,updated_at FROM vendor_documents WHERE id=?", id);
    }
    int auditCount(UUID id) { return jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE entity_id=?", Integer.class, id); }

    HttpResponse<String> send(String path, String payload, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base() + path)).timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(payload));
        if (!token.isEmpty()) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode patch(String path, String payload, String token, int expectedStatus) throws Exception {
        var before = sqlSnapshot();
        var response = send(path, payload, token);
        record(path, payload, token, response, before);
        assertThat(response.statusCode()).as("PATCH %s response=%s", path, response.body()).isEqualTo(expectedStatus);
        return json.readTree(response.body());
    }

    Map<String,Object> sqlSnapshot() {
        var snapshot = new LinkedHashMap<String,Object>();
        snapshot.put("category", categorySnapshot());
        snapshot.put("documents", jdbc.queryForList("SELECT id,vendor_id,doc_type,file_url,status,reviewed_by,reviewed_at,created_at,updated_at FROM vendor_documents WHERE vendor_id=? ORDER BY id", vendorId));
        snapshot.put("vendor", jdbc.queryForMap("SELECT id,user_id,verification_status,verified_by,verified_at FROM vendors WHERE id=?", vendorId));
        snapshot.put("audit", jdbc.queryForList("SELECT actor_user_id,action,entity_id,metadata FROM audit_logs WHERE entity_id IN (?,?) ORDER BY created_at", licenseId, safetyId));
        return snapshot;
    }

    void record(String path, String payload, String token, HttpResponse<String> response, Map<String,Object> before) throws Exception {
        record("PATCH", path, payload, token, response, before);
    }

    void assertUpdatedAt(JsonNode response, Map<String,Object> stored) {
        assertThat(OffsetDateTime.parse(response.path("updatedAt").asText()).toInstant())
                .isEqualTo(((Timestamp) stored.get("updated_at")).toInstant());
    }

    JsonNode get(String path, String token, int expectedStatus) throws Exception {
        var before = sqlSnapshot();
        var request = HttpRequest.newBuilder(URI.create(base() + path)).timeout(Duration.ofSeconds(15)).GET();
        if (!token.isEmpty()) request.header("Authorization", "Bearer " + token);
        var response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        record("GET", path, "", token, response, before);
        assertThat(response.statusCode()).as("GET %s", path).isEqualTo(expectedStatus);
        assertThat(sqlSnapshot()).isEqualTo(before);
        return json.readTree(response.body());
    }

    void record(String method, String path, String payload, String token, HttpResponse<String> response, Map<String,Object> before) throws Exception {
        var record = new LinkedHashMap<String,Object>();
        record.put("method", method); record.put("path", path); record.put("payload", payload);
        record.put("role", token.isEmpty() ? "ANONYMOUS" : token.equals(adminToken) ? "ADMIN" : token.equals(vendorToken) ? "VENDOR" : "CUSTOMER");
        record.put("status", response.statusCode()); record.put("response", json.readTree(response.body()));
        record.put("sqlBefore", before);
        record.put("sqlAfter", sqlSnapshot());
        Files.writeString(evidence, json.writeValueAsString(record) + "\n", StandardOpenOption.APPEND);
    }
}

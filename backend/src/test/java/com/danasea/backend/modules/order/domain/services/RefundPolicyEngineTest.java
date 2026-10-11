package com.danasea.backend.modules.order.domain.services;

import static org.junit.jupiter.api.Assertions.*;

import com.danasea.backend.modules.order.domain.models.RefundEvaluationResult;
import com.danasea.backend.modules.order.domain.models.RefundReason;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DisplayName("RefundPolicyEngine Comprehensive Unit Tests")
class RefundPolicyEngineTest {

    private RefundPolicyEngine engine;
    private LocalDateTime baseDeparture;
    private BigDecimal standardAmount;

    @BeforeEach
    void setUp() {
        engine = new RefundPolicyEngine();
        baseDeparture = LocalDateTime.of(2026, 10, 5, 10, 0, 0);
        standardAmount = new BigDecimal("1000000.00");
    }

    @Nested
    @DisplayName("Customer Cancellation Time Window Tests")
    class CustomerRequestTierTests {

        @Test
        @DisplayName("48h01m before departure (> 48h) -> 100.0% refund")
        void testCancel_48h01m_Returns100Percent() {
            LocalDateTime cancelTime = baseDeparture.minusHours(48).minusMinutes(1);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(100.0), result.refundPercentage());
            assertEquals(1, result.refundPercentage().scale());
            assertEquals(0, standardAmount.compareTo(result.refundAmount()));
            assertEquals(new BigDecimal("1000000.00"), result.refundAmount());
        }

        @Test
        @DisplayName("47h59m before departure (24h - 48h) -> 70.0% refund")
        void testCancel_47h59m_Returns70Percent() {
            LocalDateTime cancelTime = baseDeparture.minusHours(47).minusMinutes(59);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(70.0), result.refundPercentage());
            assertEquals(1, result.refundPercentage().scale());
            assertEquals(0, new BigDecimal("700000.00").compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("24h01m before departure (24h - 48h) -> 70.0% refund")
        void testCancel_24h01m_Returns70Percent() {
            LocalDateTime cancelTime = baseDeparture.minusHours(24).minusMinutes(1);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(70.0), result.refundPercentage());
            assertEquals(0, new BigDecimal("700000.00").compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("23h59m before departure (2h - 24h) -> 30.0% refund")
        void testCancel_23h59m_Returns30Percent() {
            LocalDateTime cancelTime = baseDeparture.minusHours(23).minusMinutes(59);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(30.0), result.refundPercentage());
            assertEquals(0, new BigDecimal("300000.00").compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("2h01m before departure (2h - 24h) -> 30.0% refund")
        void testCancel_2h01m_Returns30Percent() {
            LocalDateTime cancelTime = baseDeparture.minusHours(2).minusMinutes(1);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(30.0), result.refundPercentage());
            assertEquals(0, new BigDecimal("300000.00").compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("1h59m before departure (< 2h) -> 0.0% refund")
        void testCancel_1h59m_Returns0Percent() {
            LocalDateTime cancelTime = baseDeparture.minusHours(1).minusMinutes(59);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(0.0), result.refundPercentage());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.refundAmount()));
        }
    }

    @Nested
    @DisplayName("Exact Boundary Tests (48h00m, 24h00m, 2h00m)")
    class ExactBoundaryTests {

        @Test
        @DisplayName("Exactly 48h00m (2880 min) -> Falls into [24h, 48h] tier, returns 70.0%")
        void testBoundary_Exactly48h00m() {
            LocalDateTime cancelTime = baseDeparture.minusHours(48);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(70.0), result.refundPercentage());
            assertEquals(0, new BigDecimal("700000.00").compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("Exactly 24h00m (1440 min) -> Falls into [24h, 48h] tier, returns 70.0%")
        void testBoundary_Exactly24h00m() {
            LocalDateTime cancelTime = baseDeparture.minusHours(24);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(70.0), result.refundPercentage());
            assertEquals(0, new BigDecimal("700000.00").compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("Exactly 2h00m (120 min) -> Falls into [2h, 24h] tier, returns 30.0%")
        void testBoundary_Exactly2h00m() {
            LocalDateTime cancelTime = baseDeparture.minusHours(2);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(30.0), result.refundPercentage());
            assertEquals(0, new BigDecimal("300000.00").compareTo(result.refundAmount()));
        }
    }

    @Nested
    @DisplayName("Force Majeure & Vendor Fault Tests")
    class ForceMajeureAndSpecialReasonsTests {

        @Test
        @DisplayName("WEATHER -> Always 100.0% refund even 30 min before departure")
        void testWeather_30mBeforeDeparture_Returns100Percent() {
            LocalDateTime cancelTime = baseDeparture.minusMinutes(30);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.WEATHER, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(100.0), result.refundPercentage());
            assertEquals(0, standardAmount.compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("WEATHER -> 100.0% refund even when timestamps are null")
        void testWeather_NullTimestamps_Returns100Percent() {
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.WEATHER, null, null, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(100.0), result.refundPercentage());
            assertEquals(0, standardAmount.compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("VENDOR_FAULT -> Always 100.0% refund at any time")
        void testVendorFault_AnyTime_Returns100Percent() {
            LocalDateTime cancelTime = baseDeparture.minusMinutes(10);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.VENDOR_FAULT, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(100.0), result.refundPercentage());
            assertEquals(0, standardAmount.compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("ADMIN_OVERRIDE with custom percentage -> evaluates accurately")
        void testAdminOverride_CustomPercentage() {
            BigDecimal customRate = new BigDecimal("85.0");
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.ADMIN_OVERRIDE, baseDeparture, LocalDateTime.now(), standardAmount, customRate, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(85.0), result.refundPercentage());
            assertEquals(0, new BigDecimal("850000.00").compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("ADMIN_OVERRIDE without custom percentage -> defaults to 100.0%")
        void testAdminOverride_Default100Percent() {
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.ADMIN_OVERRIDE, baseDeparture, LocalDateTime.now(), standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(100.0), result.refundPercentage());
            assertEquals(0, standardAmount.compareTo(result.refundAmount()));
        }
    }

    @Nested
    @DisplayName("Edge Cases & Defensive Handling Tests")
    class EdgeCasesAndDefensiveTests {

        @Test
        @DisplayName("Past departure time (cancel after departure) -> 0.0% refund for customer")
        void testPastDepartureTime_Returns0Percent() {
            LocalDateTime cancelTime = baseDeparture.plusMinutes(15);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(0.0), result.refundPercentage());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("Null reason -> 0.0% refund safely")
        void testNullReason_ReturnsZero() {
            RefundEvaluationResult result = engine.evaluate(
                    null, baseDeparture, baseDeparture.minusHours(50), standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(0.0), result.refundPercentage());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("Null totalAmount -> returns 0.00 refund amount safely without NPE")
        void testNullAmount_HandledSafely() {
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, baseDeparture.minusHours(50), null, null, RefundPolicyEngine.LEGACY_POLICY);

            assertNotNull(result);
            assertEquals(BigDecimal.valueOf(100.0), result.refundPercentage());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("Zero or negative totalAmount -> returns zero refund amount safely")
        void testZeroOrNegativeAmount_HandledSafely() {
            RefundEvaluationResult zeroResult = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, baseDeparture.minusHours(50), BigDecimal.ZERO, null, RefundPolicyEngine.LEGACY_POLICY);
            assertEquals(0, BigDecimal.ZERO.compareTo(zeroResult.refundAmount()));

            RefundEvaluationResult negResult = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, baseDeparture.minusHours(50), new BigDecimal("-50000"), null, RefundPolicyEngine.LEGACY_POLICY);
            assertEquals(0, BigDecimal.ZERO.compareTo(negResult.refundAmount()));
        }

        @Test
        @DisplayName("Null departureTime or cancelTime for customer -> returns 0.0% safely")
        void testNullTimestampsForCustomer_ReturnsZero() {
            RefundEvaluationResult res1 = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, null, baseDeparture.minusHours(50), standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);
            assertEquals(BigDecimal.valueOf(0.0), res1.refundPercentage());

            RefundEvaluationResult res2 = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, null, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);
            assertEquals(BigDecimal.valueOf(0.0), res2.refundPercentage());
        }

        @Test
        @DisplayName("Legacy CUSTOMER_CANCEL enum value behaves identically to CUSTOMER_REQUEST")
        void testLegacyCustomerCancel_Compatibility() {
            LocalDateTime cancelTime = baseDeparture.minusHours(50);
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_CANCEL, baseDeparture, cancelTime, standardAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(100.0), result.refundPercentage());
            assertEquals(0, standardAmount.compareTo(result.refundAmount()));
        }

        @Test
        @DisplayName("Fractional amount rounding HALF_UP calculation")
        void testFractionalAmount_RoundingHalfUp() {
            BigDecimal fractionalAmount = new BigDecimal("1234567.89");
            LocalDateTime cancelTime = baseDeparture.minusHours(30); // 70%
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.CUSTOMER_REQUEST, baseDeparture, cancelTime, fractionalAmount, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(70.0), result.refundPercentage());
            // 1,234,567.89 * 70 / 100 = 864,197.523 -> 864,197.52
            assertEquals(new BigDecimal("864197.52"), result.refundAmount());
        }

        @Test
        @DisplayName("Scale preservation: Integer VND (scale 0) preserves scale 0 for 100% refund")
        void testScalePreservation_ScaleZeroInput() {
            BigDecimal integerVnd = BigDecimal.valueOf(1200000); // scale 0
            RefundEvaluationResult result = engine.evaluate(
                    RefundReason.WEATHER, baseDeparture, LocalDateTime.now(), integerVnd, null, RefundPolicyEngine.LEGACY_POLICY);

            assertEquals(BigDecimal.valueOf(100.0), result.refundPercentage());
            assertEquals(integerVnd, result.refundAmount());
            assertEquals(0, result.refundAmount().scale());
            assertTrue(integerVnd.equals(result.refundAmount()));
        }
    }

    @Nested
    @DisplayName("Policy Text Description & Summaries")
    class PolicyTextTests {

        @Test
        @DisplayName("getRefundPolicySummary contains required substring for REFUND")
        void testRefundPolicySummary_ContainsRequiredSubstring() {
            String summary = engine.getRefundPolicySummary();
            assertTrue(summary.contains("Refund policy:"));

            String refundSummary = engine.getRefundPolicySummary("REFUND");
            assertTrue(refundSummary.contains("Refund policy:"));

            String cancellationSummary = engine.getRefundPolicySummary("CANCELLATION");
            assertTrue(cancellationSummary.contains("100% more than 24 hours"));
            assertTrue(cancellationSummary.contains("0% within 24 hours"));
        }

        @Test
        @DisplayName("getPolicyDescription returns accurate descriptions for all reasons")
        void testPolicyDescription_AccurateDescriptions() {
            assertTrue(engine.getPolicyDescription(RefundReason.WEATHER, BigDecimal.valueOf(100.0))
                    .contains("weather"));
            assertTrue(engine.getPolicyDescription(RefundReason.VENDOR_FAULT, BigDecimal.valueOf(100.0))
                    .contains("vendor"));
            assertTrue(engine.getPolicyDescription(RefundReason.ADMIN_OVERRIDE, BigDecimal.valueOf(100.0))
                    .contains("administrator"));
            assertTrue(engine.getPolicyDescription(RefundReason.CUSTOMER_REQUEST, BigDecimal.valueOf(100.0))
                    .contains("more than 24 hours"));
            assertTrue(engine.getPolicyDescription(RefundReason.CUSTOMER_REQUEST, BigDecimal.valueOf(70.0))
                    .contains("24 to 48 hours"));
            assertTrue(engine.getPolicyDescription(RefundReason.CUSTOMER_REQUEST, BigDecimal.valueOf(30.0))
                    .contains("2 to 24 hours"));
            assertTrue(engine.getPolicyDescription(RefundReason.CUSTOMER_REQUEST, BigDecimal.valueOf(0.0))
                    .contains("No refund"));
        }
    }
}

package com.danasea.backend.modules.order.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.danasea.backend.modules.order.domain.models.RefundReason;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

class StandardCancellationPolicyTest {
    private final RefundPolicyEngine engine = new RefundPolicyEngine();
    private final LocalDateTime departure = LocalDateTime.of(2030, 5, 10, 8, 0);
    private final BigDecimal paid = new BigDecimal("900000.00");

    @ParameterizedTest
    @ValueSource(longs = {25, 48, 72})
    void cancellationMoreThan24HoursRefundsAllPaidMoney(long hours) {
        var result =
                engine.evaluate(
                        RefundReason.CUSTOMER_CANCEL, departure, departure.minusHours(hours), paid);
        assertThat(result.refundPercentage()).isEqualByComparingTo("100");
        assertThat(result.refundAmount()).isEqualByComparingTo(paid);
    }

    @Test
    void oneNanosecondOutside24HoursIsStillFullyRefundable() {
        assertThat(
                        engine.evaluate(
                                        RefundReason.CUSTOMER_REQUEST,
                                        departure,
                                        departure.minusHours(24).minusNanos(1),
                                        paid)
                                .refundAmount())
                .isEqualByComparingTo(paid);
    }

    @ParameterizedTest
    @ValueSource(longs = {24, 12, 2, 0, -1})
    void atOrWithin24HoursAndAfterDepartureDoesNotRefund(long hours) {
        assertThat(
                        engine.evaluate(
                                        RefundReason.CUSTOMER_CANCEL,
                                        departure,
                                        departure.minusHours(hours),
                                        paid)
                                .refundAmount())
                .isZero();
    }

    @ParameterizedTest
    @EnumSource(
            value = RefundReason.class,
            names = {"WEATHER", "VENDOR_FAULT", "SYSTEM_FAULT"})
    void verifiedNonCustomerReasonsOverrideTheCustomerCutoff(RefundReason reason) {
        assertThat(
                        engine.evaluate(reason, departure, departure.minusMinutes(1), paid)
                                .refundAmount())
                .isEqualByComparingTo(paid);
    }

    @Test
    void savedLegacyPolicyRetainsItsPreviouslyDisclosedRefund() {
        assertThat(
                        engine.evaluate(
                                        RefundReason.CUSTOMER_CANCEL,
                                        departure,
                                        departure.minusHours(36),
                                        paid,
                                        null,
                                        RefundPolicyEngine.LEGACY_POLICY)
                                .refundPercentage())
                .isEqualByComparingTo("70");
        assertThat(
                        engine.evaluate(
                                        RefundReason.CUSTOMER_CANCEL,
                                        departure,
                                        departure.minusHours(12),
                                        paid,
                                        null,
                                        RefundPolicyEngine.LEGACY_POLICY)
                                .refundPercentage())
                .isEqualByComparingTo("30");
    }
}

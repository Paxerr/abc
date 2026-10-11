package com.danasea.backend.modules.order.domain.services;

import com.danasea.backend.modules.order.domain.models.RefundEvaluationResult;
import com.danasea.backend.modules.order.domain.models.RefundReason;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class RefundPolicyEngine {
    public static final String STANDARD_POLICY = "STANDARD_24H_V2";
    public static final String LEGACY_POLICY = "LEGACY_TIERED_V1";

    public RefundEvaluationResult evaluate(
            RefundReason reason,
            LocalDateTime departureTime,
            LocalDateTime cancelTime,
            BigDecimal totalAmount) {
        return evaluate(reason, departureTime, cancelTime, totalAmount, null);
    }

    public RefundEvaluationResult evaluate(
            RefundReason reason,
            LocalDateTime departureTime,
            LocalDateTime cancelTime,
            BigDecimal totalAmount,
            BigDecimal customPercentage) {
        return evaluate(
                reason, departureTime, cancelTime, totalAmount, customPercentage, STANDARD_POLICY);
    }

    public RefundEvaluationResult evaluate(
            RefundReason reason,
            LocalDateTime departureTime,
            LocalDateTime cancelTime,
            BigDecimal totalAmount,
            BigDecimal customPercentage,
            String policyVersion) {
        if (reason == null) {
            BigDecimal percentage = BigDecimal.valueOf(0.0);
            return result(reason, percentage, totalAmount);
        }

        // 1. Force majeure and vendor fault: always 100.0% refund, ignoring time
        if (reason == RefundReason.WEATHER
                || reason == RefundReason.VENDOR_FAULT
                || reason == RefundReason.SYSTEM_FAULT) {
            BigDecimal percentage = BigDecimal.valueOf(100.0);
            return result(reason, percentage, totalAmount);
        }

        // 2. Admin override, dispute, compensation: use custom percentage if provided, otherwise
        // default to 100.0%
        if (reason == RefundReason.ADMIN_OVERRIDE
                || reason == RefundReason.COMPENSATION
                || reason == RefundReason.DISPUTE) {
            BigDecimal percentage =
                    customPercentage != null
                            ? customPercentage.setScale(1, RoundingMode.HALF_UP)
                            : BigDecimal.valueOf(100.0);
            return result(reason, percentage, totalAmount);
        }

        // 3. Customer-initiated cancellation: time-based tiered policy
        if (reason == RefundReason.CUSTOMER_REQUEST || reason == RefundReason.CUSTOMER_CANCEL) {
            if (departureTime == null || cancelTime == null || cancelTime.isAfter(departureTime)) {
                BigDecimal percentage = BigDecimal.valueOf(0.0);
                return result(reason, percentage, totalAmount);
            }

            Duration remaining = Duration.between(cancelTime, departureTime);
            if (!LEGACY_POLICY.equals(policyVersion)) {
                return result(
                        reason,
                        remaining.compareTo(Duration.ofHours(24)) > 0
                                ? BigDecimal.valueOf(100.0)
                                : BigDecimal.valueOf(0.0),
                        totalAmount);
            }
            long minutesUntilDeparture = remaining.toMinutes();
            BigDecimal percentage;

            if (minutesUntilDeparture > 2880) {
                // > 48 hours: 100%
                percentage = BigDecimal.valueOf(100.0);
            } else if (minutesUntilDeparture >= 1440) {
                // 24 hours to 48 hours: 70%
                percentage = BigDecimal.valueOf(70.0);
            } else if (minutesUntilDeparture >= 120) {
                // 2 hours to 24 hours: 30%
                percentage = BigDecimal.valueOf(30.0);
            } else {
                // < 2 hours or past: 0%
                percentage = BigDecimal.valueOf(0.0);
            }

            return result(reason, percentage, totalAmount);
        }

        // Default fallback
        BigDecimal percentage = BigDecimal.valueOf(0.0);
        return result(reason, percentage, totalAmount);
    }

    private RefundEvaluationResult result(
            RefundReason reason, BigDecimal percentage, BigDecimal totalAmount) {
        String code;
        if (reason == RefundReason.WEATHER) {
            code = "WEATHER";
        } else if (reason == RefundReason.SYSTEM_FAULT) {
            code = "SYSTEM_FAULT";
        } else if (reason == RefundReason.VENDOR_FAULT) {
            code = "VENDOR_FAULT";
        } else if (reason == RefundReason.ADMIN_OVERRIDE
                || reason == RefundReason.COMPENSATION
                || reason == RefundReason.DISPUTE) {
            code = "ADMIN_OVERRIDE";
        } else {
            code = percentage == null ? "0" : percentage.stripTrailingZeros().toPlainString();
        }
        return new RefundEvaluationResult(
                percentage, calculateAmount(totalAmount, percentage), code);
    }

    private BigDecimal calculateAmount(BigDecimal totalAmount, BigDecimal percentage) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Backwards compatibility for existing unit tests:
        // When input has scale 0 (e.g. integer VND) and refund is 100%, preserve scale 0
        if (percentage.compareTo(BigDecimal.valueOf(100.0)) == 0 && totalAmount.scale() == 0) {
            return totalAmount;
        }

        if (percentage.compareTo(BigDecimal.ZERO) == 0) {
            return totalAmount.scale() == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return totalAmount
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP);
    }

    public String getRefundPolicySummary() {
        return "Refund policy: 100% more than 24 hours before departure and 0% within 24 hours or"
                   + " for no-shows. Verified dangerous weather, vendor fault and system fault"
                   + " receive a full refund.";
    }

    public String getRefundPolicySummary(String policyType) {
        if (policyType == null) {
            return getRefundPolicySummary();
        }
        return switch (policyType.toUpperCase()) {
            case "REFUND", "CANCELLATION" -> getRefundPolicySummary();
            default -> getRefundPolicySummary();
        };
    }

    public String getCancellationPolicySummary() {
        return getRefundPolicySummary();
    }

    public String getPolicyDescription(
            RefundReason reason, BigDecimal refundPercentage, String policyVersion) {
        if (LEGACY_POLICY.equals(policyVersion)
                && (reason == RefundReason.CUSTOMER_CANCEL
                        || reason == RefundReason.CUSTOMER_REQUEST)) {
            return "Original booking policy: 100% more than 48 hours before departure, 70% from 24"
                       + " to 48 hours, 30% from 2 to 24 hours, and 0% within 2 hours or after"
                       + " departure.";
        }
        return getPolicyDescription(reason, refundPercentage);
    }

    public String getPolicyDescription(RefundReason reason, BigDecimal refundPercentage) {
        if (reason == RefundReason.WEATHER) {
            return "Full refund due to dangerous marine weather";
        }
        if (reason == RefundReason.SYSTEM_FAULT) {
            return "Full refund due to a verified system fault";
        }
        if (reason == RefundReason.VENDOR_FAULT) {
            return "Full refund due to vendor fault";
        }
        if (reason == RefundReason.ADMIN_OVERRIDE) {
            return "Refund determined by an administrator";
        }
        if (refundPercentage != null) {
            if (refundPercentage.compareTo(BigDecimal.valueOf(100.0)) == 0) {
                return "100% refund because cancellation occurred more than 24 hours before"
                           + " departure";
            } else if (refundPercentage.compareTo(BigDecimal.valueOf(70.0)) == 0) {
                return "70% refund because cancellation occurred 24 to 48 hours before departure";
            } else if (refundPercentage.compareTo(BigDecimal.valueOf(30.0)) == 0) {
                return "30% refund because cancellation occurred 2 to 24 hours before departure";
            }
        }
        return "No refund because cancellation occurred within 24 hours of departure, after"
                   + " departure or for a no-show";
    }
}

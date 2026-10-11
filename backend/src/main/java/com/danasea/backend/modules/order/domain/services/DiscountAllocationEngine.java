package com.danasea.backend.modules.order.domain.services;

import com.danasea.backend.modules.order.domain.models.DiscountCode;
import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.domain.models.DiscountSponsorType;
import com.danasea.backend.modules.order.domain.models.DiscountType;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DiscountAllocationEngine {

    public static final int MONEY_SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final MathContext MC = MathContext.DECIMAL64;

    public record CandidateItem(
            UUID itemId,
            UUID vendorId,
            UUID serviceId,
            BigDecimal subtotalAmount,
            BigDecimal commissionRate
    ) {}

    public record ItemAllocation(
            UUID itemId,
            UUID vendorId,
            UUID serviceId,
            BigDecimal originalSubtotal,
            boolean eligible,
            BigDecimal vendorDiscountAmount,
            BigDecimal platformDiscountAmount,
            BigDecimal totalDiscountAmount,
            BigDecimal finalSubtotal,
            BigDecimal commissionBasisAmount,
            BigDecimal commissionAmount,
            BigDecimal vendorPayoutAmount
    ) {}

    public record AllocationResult(
            boolean valid,
            String errorCode,
            String message,
            DiscountCode discountCode,
            BigDecimal totalEligibleAmount,
            BigDecimal totalDiscountAmount,
            BigDecimal finalPayableAmount,
            List<ItemAllocation> itemAllocations
    ) {
        public static AllocationResult invalid(String errorCode, String message) {
            return new AllocationResult(
                    false,
                    errorCode,
                    message,
                    null,
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE),
                    Collections.emptyList()
            );
        }
    }

    private static class QuotaItem {
        final CandidateItem item;
        final BigDecimal exactShare;
        BigDecimal allocatedShare;
        final BigDecimal remainder;

        QuotaItem(CandidateItem item, BigDecimal exactShare, BigDecimal allocatedShare, BigDecimal remainder) {
            this.item = item;
            this.exactShare = exactShare;
            this.allocatedShare = allocatedShare;
            this.remainder = remainder;
        }
    }

    public AllocationResult evaluateAndAllocate(
            DiscountCode discountCode,
            List<CandidateItem> items,
            OffsetDateTime now,
            long customerUsageCount
    ) {
        return evaluateAndAllocate(discountCode, items, now, customerUsageCount, null);
    }

    public AllocationResult evaluateAndAllocate(
            DiscountCode discountCode,
            List<CandidateItem> items,
            OffsetDateTime now,
            long customerUsageCount,
            UUID customerId
    ) {
        if (discountCode == null) {
            return AllocationResult.invalid("DISCOUNT_NOT_FOUND", "Discount code was not found.");
        }

        if (discountCode.getRecipientCustomerId() != null
                && !discountCode.getRecipientCustomerId().equals(customerId)) {
            return AllocationResult.invalid("DISCOUNT_CUSTOMER_RESTRICTED", "Discount code belongs to another customer.");
        }

        if (Boolean.FALSE.equals(discountCode.getIsActive())) {
            return AllocationResult.invalid("DISCOUNT_INACTIVE", "Discount code is inactive.");
        }

        OffsetDateTime evaluationTime = now != null ? now : OffsetDateTime.now();
        if (discountCode.getValidFrom() != null && evaluationTime.isBefore(discountCode.getValidFrom())) {
            return AllocationResult.invalid("DISCOUNT_NOT_STARTED", "Discount validity has not started.");
        }
        if (discountCode.getValidTo() != null && evaluationTime.isAfter(discountCode.getValidTo())) {
            return AllocationResult.invalid("DISCOUNT_EXPIRED", "Discount code has expired.");
        }

        if (!discountCode.hasQuotaRemaining()) {
            return AllocationResult.invalid("DISCOUNT_QUOTA_EXCEEDED", "Discount usage quota is exhausted.");
        }

        if (!discountCode.canUserRedeem(customerUsageCount)) {
            return AllocationResult.invalid("DISCOUNT_USER_LIMIT_EXCEEDED", "Customer discount usage limit is exhausted.");
        }

        if (items == null || items.isEmpty()) {
            return AllocationResult.invalid("EMPTY_ITEMS", "Booking has no items.");
        }

        // 1. Phân loại item đủ điều kiện (Eligibility)
        List<CandidateItem> eligibleItems = new ArrayList<>();
        List<CandidateItem> nonEligibleItems = new ArrayList<>();

        for (CandidateItem item : items) {
            boolean eligible = isItemEligible(discountCode, item);
            if (eligible) {
                eligibleItems.add(item);
            } else {
                nonEligibleItems.add(item);
            }
        }

        if (eligibleItems.isEmpty()) {
            return AllocationResult.invalid("DISCOUNT_SCOPE_MISMATCH", "Discount does not apply to any booking item.");
        }

        // 2. Tính tổng giá trị đủ điều kiện
        BigDecimal totalEligibleAmount = eligibleItems.stream()
                .map(CandidateItem::subtotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, ROUNDING_MODE);

        if (totalEligibleAmount.signum() <= 0) {
            return AllocationResult.invalid("DISCOUNT_MIN_AMOUNT_NOT_MET", "Eligible amount must be positive.");
        }
        if (!discountCode.meetsMinimumAmount(totalEligibleAmount)) {
            return AllocationResult.invalid("DISCOUNT_MIN_AMOUNT_NOT_MET",
                    "Eligible amount does not meet the discount minimum.");
        }

        // 3. Tính tổng tiền giảm giá
        BigDecimal totalDiscountAmount;
        if (discountCode.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal rawPercentDiscount = totalEligibleAmount
                    .multiply(discountCode.getDiscountValue(), MC)
                    .divide(BigDecimal.valueOf(100), MC)
                    .setScale(MONEY_SCALE, ROUNDING_MODE);

            if (discountCode.getMaxDiscountAmount() != null
                    && discountCode.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                totalDiscountAmount = rawPercentDiscount.min(discountCode.getMaxDiscountAmount().setScale(MONEY_SCALE, ROUNDING_MODE));
            } else {
                totalDiscountAmount = rawPercentDiscount;
            }
        } else {
            // FIXED
            totalDiscountAmount = discountCode.getDiscountValue().setScale(MONEY_SCALE, ROUNDING_MODE);
        }

        // Giảm tối đa bằng tổng giá trị các item đủ điều kiện
        totalDiscountAmount = totalDiscountAmount.min(totalEligibleAmount).setScale(MONEY_SCALE, ROUNDING_MODE);

        // 4. Giải thuật phân bổ Largest Remainder (Hare-Niemeyer)
        List<QuotaItem> quotaItems = new ArrayList<>();
        BigDecimal sumFloored = BigDecimal.ZERO;

        for (CandidateItem item : eligibleItems) {
            BigDecimal exactShare = totalDiscountAmount
                    .multiply(item.subtotalAmount(), MC)
                    .divide(totalEligibleAmount, MC);

            BigDecimal flooredShare = exactShare.setScale(MONEY_SCALE, RoundingMode.FLOOR);
            BigDecimal remainder = exactShare.subtract(flooredShare);

            quotaItems.add(new QuotaItem(item, exactShare, flooredShare, remainder));
            sumFloored = sumFloored.add(flooredShare);
        }

        BigDecimal difference = totalDiscountAmount.subtract(sumFloored).setScale(MONEY_SCALE, ROUNDING_MODE);
        int unitsToDistribute = difference.movePointRight(MONEY_SCALE).intValue();

        // Sắp xếp theo phần dư giảm dần, nếu bằng nhau sắp theo ID ổn định
        quotaItems.sort(Comparator
                .comparing((QuotaItem qi) -> qi.remainder).reversed()
                .thenComparing(qi -> qi.item.itemId() != null ? qi.item.itemId().toString() : ""));

        BigDecimal unitValue = BigDecimal.ONE.movePointLeft(MONEY_SCALE);
        for (int i = 0; i < unitsToDistribute && i < quotaItems.size(); i++) {
            QuotaItem qi = quotaItems.get(i);
            qi.allocatedShare = qi.allocatedShare.add(unitValue);
        }

        // 5. Tổng hợp Item Allocations
        DiscountSponsorType sponsor = discountCode.getSponsorType() != null
                ? discountCode.getSponsorType()
                : (discountCode.getScope() == DiscountScope.VENDOR ? DiscountSponsorType.VENDOR : DiscountSponsorType.PLATFORM);

        List<ItemAllocation> allocations = new ArrayList<>();

        for (QuotaItem qi : quotaItems) {
            CandidateItem item = qi.item;
            BigDecimal itemDiscount = qi.allocatedShare.min(item.subtotalAmount());

            BigDecimal vendorDisc = sponsor == DiscountSponsorType.VENDOR ? itemDiscount : BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal platformDisc = sponsor == DiscountSponsorType.PLATFORM ? itemDiscount : BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);

            BigDecimal finalSub = item.subtotalAmount().subtract(itemDiscount).setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal commBasis = item.subtotalAmount().subtract(vendorDisc).setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal rate = item.commissionRate() != null ? item.commissionRate() : BigDecimal.ZERO;
            BigDecimal commAmount = commBasis.multiply(rate).setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal vendorPayout = commBasis.subtract(commAmount).setScale(MONEY_SCALE, ROUNDING_MODE);

            allocations.add(new ItemAllocation(
                    item.itemId(),
                    item.vendorId(),
                    item.serviceId(),
                    item.subtotalAmount().setScale(MONEY_SCALE, ROUNDING_MODE),
                    true,
                    vendorDisc,
                    platformDisc,
                    itemDiscount,
                    finalSub,
                    commBasis,
                    commAmount,
                    vendorPayout
            ));
        }

        for (CandidateItem item : nonEligibleItems) {
            BigDecimal commBasis = item.subtotalAmount().setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal rate = item.commissionRate() != null ? item.commissionRate() : BigDecimal.ZERO;
            BigDecimal commAmount = commBasis.multiply(rate).setScale(MONEY_SCALE, ROUNDING_MODE);
            BigDecimal vendorPayout = commBasis.subtract(commAmount).setScale(MONEY_SCALE, ROUNDING_MODE);

            allocations.add(new ItemAllocation(
                    item.itemId(),
                    item.vendorId(),
                    item.serviceId(),
                    item.subtotalAmount().setScale(MONEY_SCALE, ROUNDING_MODE),
                    false,
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE),
                    item.subtotalAmount().setScale(MONEY_SCALE, ROUNDING_MODE),
                    commBasis,
                    commAmount,
                    vendorPayout
            ));
        }

        BigDecimal allItemsTotal = items.stream()
                .map(CandidateItem::subtotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, ROUNDING_MODE);

        BigDecimal finalPayableAmount = allItemsTotal.subtract(totalDiscountAmount).max(BigDecimal.ZERO).setScale(MONEY_SCALE, ROUNDING_MODE);

        return new AllocationResult(
                true,
                null,
                "Discount applied successfully.",
                discountCode,
                totalEligibleAmount,
                totalDiscountAmount,
                finalPayableAmount,
                allocations
        );
    }

    private boolean isItemEligible(DiscountCode discountCode, CandidateItem item) {
        if (discountCode.getScope() == DiscountScope.VENDOR) {
            if (discountCode.getVendorId() == null || !discountCode.getVendorId().equals(item.vendorId())) {
                return false;
            }
        }
        if (discountCode.getServiceId() != null) {
            if (!discountCode.getServiceId().equals(item.serviceId())) {
                return false;
            }
        }
        return true;
    }
}

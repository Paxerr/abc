package com.danasea.backend.modules.order.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.danasea.backend.e2e.BaseE2ETest;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingItemJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.JpaBookingItemRepository;
import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.JpaBookingRepository;
import com.danasea.backend.modules.order.application.DiscountReservationService;
import com.danasea.backend.modules.order.application.dtos.CreateOrderCommand;
import com.danasea.backend.modules.order.application.dtos.GetCancellationPreviewQuery;
import com.danasea.backend.modules.order.application.dtos.RequestRefundCommand;
import com.danasea.backend.modules.order.application.services.DisruptionDecisionService;
import com.danasea.backend.modules.order.domain.exceptions.InvalidDiscountException;
import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.domain.models.DiscountType;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.jobs.DisruptionDecisionJob;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.DiscountCodeJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.DiscountRedemptionJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderDisruptionJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaDiscountCodeRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaDiscountRedemptionRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderDisruptionRepository;
import com.danasea.backend.modules.order.presentation.dtos.ConfirmRescheduleRequest;
import com.danasea.backend.modules.order.presentation.dtos.DiscountPreviewRequest;
import com.danasea.backend.modules.weather.application.services.WeatherBookingCancellationService;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest(
        properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("test")
class DisruptionDecisionIntegrationTest extends BaseE2ETest {
    @MockitoBean LettuceBasedProxyManager<byte[]> proxyManager;
    @Autowired DisruptionDecisionService decisions;
    @Autowired JpaSubOrderDisruptionRepository disruptions;
    @Autowired JpaRefundRepository refunds;
    @Autowired JpaDiscountCodeRepository codes;
    @Autowired JpaDiscountRedemptionRepository redemptions;
    @Autowired DiscountReservationService reservations;
    @Autowired JpaBookingRepository bookings;
    @Autowired JpaBookingItemRepository items;
    @Autowired CreateOrderUseCase checkout;
    @Autowired DiscountPreviewUseCase preview;
    @Autowired ConfirmSubOrderRescheduleUseCase reschedule;
    @Autowired GetCancellationPreviewUseCase cancellationPreview;
    @Autowired RequestRefundUseCase refundRequest;
    @Autowired JpaSafetyRuleEvaluationRepository weather;
    @Autowired DisruptionDecisionJob decisionJob;
    @Autowired WeatherBookingCancellationService weatherCancellation;
    DiscountCodeJpaEntity originalCode;
    UUID subId;

    @BeforeEach
    void prepareDecision() {
        var departure = OffsetDateTime.now(ZoneOffset.ofHours(7)).plusHours(48);
        slotA1TomorrowMorning.setDate(departure.toLocalDate());
        slotA1TomorrowMorning.setStartTime(departure.toLocalTime().withNano(0));
        slotA1TomorrowMorning.setEndTime(departure.plusHours(2).toLocalTime().withNano(0));
        slotA1TomorrowMorning.setBookedCount(1);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowMorning);
        slotA1TomorrowAfternoon.setDate(departure.plusDays(1).toLocalDate());
        slotA1TomorrowAfternoon.setBookedCount(0);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowAfternoon);
        var booking = new BookingJpaEntity();
        booking.setCustomerId(customerUser.getId());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(BigDecimal.valueOf(100000));
        booking = bookings.saveAndFlush(booking);
        var item = bookingItem(booking, slotA1TomorrowMorning.getId());
        item = items.saveAndFlush(item);
        paidOrder.setBookingId(booking.getId());
        originalCode = new DiscountCodeJpaEntity();
        originalCode.setCode("EXPIRED-" + UUID.randomUUID());
        originalCode.setScope(DiscountScope.PLATFORM);
        originalCode.setDiscountType(DiscountType.FIXED);
        originalCode.setDiscountValue(BigDecimal.valueOf(10000));
        originalCode.setMaxUses(100);
        originalCode.setMaxUsesPerUser(1);
        originalCode.setUsedCount(1);
        originalCode.setValidFrom(OffsetDateTime.now().minusDays(2));
        originalCode.setValidTo(OffsetDateTime.now().minusDays(1));
        originalCode = codes.saveAndFlush(originalCode);
        paidOrder.setDiscountCodeId(originalCode.getId());
        paidOrder.setDiscountAmount(BigDecimal.valueOf(10000));
        masterOrderRepository.saveAndFlush(paidOrder);
        paidSubOrder.setBookingItemId(item.getId());
        paidSubOrder.setSlotId(slotA1TomorrowMorning.getId());
        paidSubOrder.setQuantity(1);
        paidSubOrder.setSubtotalAmount(BigDecimal.valueOf(100000));
        paidSubOrder.setDiscountAmount(BigDecimal.valueOf(10000));
        paidSubOrder.setFinalAmount(BigDecimal.valueOf(90000));
        paidSubOrder = subOrderRepository.saveAndFlush(paidSubOrder);
        subId = paidSubOrder.getId();
        var redemption = new DiscountRedemptionJpaEntity();
        redemption.setMasterOrderId(paidOrder.getId());
        redemption.setDiscountCodeId(originalCode.getId());
        redemption.setCustomerId(customerUser.getId());
        redemption.setAmountDeducted(BigDecimal.valueOf(10000));
        redemptions.saveAndFlush(redemption);
    }

    @Test
    void deadlineIsFrozenAtOriginalDepartureMinus12Hours() {
        var disruption = record(RefundReason.SYSTEM_FAULT);
        assertThat(disruption.getStatus()).isEqualTo("AWAITING_DECISION");
        assertThat(disruption.getDecisionDeadline())
                .isEqualTo(disruption.getOriginalDepartureAt().minusHours(12));
        assertThat(refunds.findBySubOrderId(subId)).isEmpty();
        decisions.expire(disruption.getId(), disruption.getDecisionDeadline().minusNanos(1));
        assertThat(refunds.findBySubOrderId(subId)).isEmpty();
        decisions.expire(disruption.getId(), disruption.getDecisionDeadline());
        decisions.expire(disruption.getId(), disruption.getDecisionDeadline().plusHours(1));
        assertThat(refunds.findBySubOrderId(subId)).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(
            value = RefundReason.class,
            names = {"WEATHER", "VENDOR_FAULT", "SYSTEM_FAULT"})
    void fullRefundRetainsOriginalVoucherAndReplacementIsOnlyForSystemFault(RefundReason reason) {
        record(reason);
        decisions.chooseRefund(subId, customerUser.getId());
        decisions.chooseRefund(subId, customerUser.getId());
        var refund = refunds.findBySubOrderId(subId).getFirst();
        assertThat(refund.getAmount()).isEqualByComparingTo("90000");
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(refund.getReason()).isEqualTo(reason);
        assertThat(
                        serviceSlotRepository
                                .findById(slotA1TomorrowMorning.getId())
                                .orElseThrow()
                                .getBookedCount())
                .isZero();
        reservations.release(paidOrder.getId(), originalCode.getId());
        assertThat(codes.findById(originalCode.getId()).orElseThrow().getUsedCount()).isEqualTo(1);
        assertThat(codes.findById(originalCode.getId()).orElseThrow().getValidTo())
                .isEqualTo(originalCode.getValidTo());
        assertThat(redemptions.findByMasterOrderId(paidOrder.getId())).hasSize(1);
        assertThat(codes.findByReplacementSubOrderId(subId).isPresent())
                .isEqualTo(reason == RefundReason.SYSTEM_FAULT);
    }

    @Test
    void systemReplacementIsPersonalSingleUseAndEqualToAffectedAllocatedDiscount() {
        record(RefundReason.SYSTEM_FAULT);
        decisions.chooseRefund(subId, customerUser.getId());
        var code = codes.findByReplacementSubOrderId(subId).orElseThrow();
        assertThat(code.getDiscountValue()).isEqualByComparingTo("10000");
        assertThat(code.getRecipientCustomerId()).isEqualTo(customerUser.getId());
        assertThat(code.getMaxUses()).isEqualTo(1);
        assertThat(code.getValidTo()).isEqualTo(code.getValidFrom().plusDays(30));
        UUID attackerHold = newHold(attackerUser.getId());
        assertThat(
                        preview.execute(
                                        attackerUser.getId(),
                                        new DiscountPreviewRequest(attackerHold, code.getCode()))
                                .errorCode())
                .isEqualTo("DISCOUNT_CUSTOMER_RESTRICTED");
        assertThatThrownBy(
                        () ->
                                checkout.execute(
                                        new CreateOrderCommand(
                                                attackerUser.getId(),
                                                attackerHold,
                                                "attacker-checkout-" + attackerHold,
                                                code.getCode())))
                .isInstanceOf(InvalidDiscountException.class);
        UUID ownHold = newHold(customerUser.getId());
        var order =
                checkout.execute(
                        new CreateOrderCommand(
                                customerUser.getId(),
                                ownHold,
                                "own-checkout-" + ownHold,
                                code.getCode()));
        assertThat(order.totalAmount()).isEqualByComparingTo("90000");
        UUID secondHold = newHold(customerUser.getId());
        assertThatThrownBy(
                        () ->
                                checkout.execute(
                                        new CreateOrderCommand(
                                                customerUser.getId(),
                                                secondHold,
                                                "second-checkout-" + secondHold,
                                                code.getCode())))
                .isInstanceOf(InvalidDiscountException.class);
        assertThat(codes.findById(code.getId()).orElseThrow().getUsedCount()).isEqualTo(1);
        assertThat(codes.findByRecipientCustomerId(attackerUser.getId(), PageRequest.of(0, 20)))
                .isEmpty();
    }

    @Test
    void anExpiredReplacementCannotBeApplied() {
        record(RefundReason.SYSTEM_FAULT);
        decisions.chooseRefund(subId, customerUser.getId());
        var code = codes.findByReplacementSubOrderId(subId).orElseThrow();
        code.setValidFrom(OffsetDateTime.now().minusDays(31));
        code.setValidTo(OffsetDateTime.now().minusDays(1));
        codes.saveAndFlush(code);
        var response =
                preview.execute(
                        customerUser.getId(),
                        new DiscountPreviewRequest(newHold(customerUser.getId()), code.getCode()));
        assertThat(response.errorCode()).isEqualTo("DISCOUNT_EXPIRED");
    }

    @Test
    void alreadyInside12HoursRefundsImmediately() {
        var departure = OffsetDateTime.now(ZoneOffset.ofHours(7)).plusHours(11);
        slotA1TomorrowMorning.setDate(departure.toLocalDate());
        slotA1TomorrowMorning.setStartTime(departure.toLocalTime().withNano(0));
        serviceSlotRepository.saveAndFlush(slotA1TomorrowMorning);
        assertThat(record(RefundReason.SYSTEM_FAULT).getStatus()).isEqualTo("REFUND_REQUESTED");
        assertThat(refunds.findBySubOrderId(subId)).hasSize(1);
    }

    @Test
    void noAllocatedDiscountDoesNotCreateAZeroValueReplacement() {
        paidSubOrder.setDiscountAmount(BigDecimal.ZERO);
        subOrderRepository.saveAndFlush(paidSubOrder);
        record(RefundReason.SYSTEM_FAULT);
        decisions.chooseRefund(subId, customerUser.getId());
        assertThat(codes.findByReplacementSubOrderId(subId)).isEmpty();
    }

    @Test
    void anotherCustomerCannotChooseForThisOrder() {
        record(RefundReason.SYSTEM_FAULT);
        assertThatThrownBy(() -> decisions.chooseRefund(subId, attackerUser.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(refunds.findBySubOrderId(subId)).isEmpty();
    }

    @Test
    void unverifiedCustomerClaimCannotBecomeASystemFaultRefund() throws Exception {
        mockMvc.perform(
                        post("/api/admin/sub-orders/{id}/disruptions", subId)
                                .header("Authorization", "Bearer " + customerToken)
                                .header("Idempotency-Key", "verify-fault-123")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"reason\":\"SYSTEM_FAULT\",\"details\":\"Claimed"
                                                + " fault\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post("/api/sub-orders/{id}/disruption-refund", subId)
                                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict());
        assertThat(refunds.findBySubOrderId(subId)).isEmpty();
    }

    @Test
    void verifiedAdminFaultAndPersonalVoucherListingAreUsableThroughHttp() throws Exception {
        mockMvc.perform(
                        post("/api/admin/sub-orders/{id}/disruptions", subId)
                                .header("Authorization", "Bearer " + adminToken)
                                .header("Idempotency-Key", "verify-fault-123")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"reason\":\"SYSTEM_FAULT\",\"details\":\"Verified"
                                                + " scheduling failure\"}"))
                .andExpect(status().isAccepted());
        mockMvc.perform(
                        post("/api/sub-orders/{id}/disruption-refund", subId)
                                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isAccepted());
        var own =
                mockMvc.perform(
                                get("/api/customers/me/discount-codes")
                                        .header("Authorization", "Bearer " + customerToken))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(
                        objectMapper
                                .readTree(own.getResponse().getContentAsString())
                                .get("totalElements")
                                .asInt())
                .isEqualTo(1);
        var other =
                mockMvc.perform(
                                get("/api/customers/me/discount-codes")
                                        .header("Authorization", "Bearer " + attackerToken))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(
                        objectMapper
                                .readTree(other.getResponse().getContentAsString())
                                .get("totalElements")
                                .asInt())
                .isZero();
    }

    @Test
    void concurrentAutomaticAndCustomerRefundProduceOneRefundAndOneVoucher() throws Exception {
        var disruption = record(RefundReason.SYSTEM_FAULT);
        var latch = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first =
                    pool.submit(
                            () -> {
                                latch.await();
                                decisions.chooseRefund(subId, customerUser.getId());
                                return true;
                            });
            var second =
                    pool.submit(
                            () -> {
                                latch.await();
                                decisions.expire(
                                        disruption.getId(), disruption.getDecisionDeadline());
                                return true;
                            });
            latch.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(15, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(refunds.findBySubOrderId(subId)).hasSize(1);
        assertThat(
                        codes.findByRecipientCustomerId(customerUser.getId(), PageRequest.of(0, 20))
                                .getTotalElements())
                .isEqualTo(1);
    }

    @Test
    void acceptedRescheduleStopsAutomaticRefundAndDoesNotIssueVoucher() {
        var disruption = record(RefundReason.SYSTEM_FAULT);
        authenticateCustomer();
        reschedule.execute(
                subId,
                new ConfirmRescheduleRequest(slotA1TomorrowAfternoon.getId(), null, 0L),
                "reschedule-system-123");
        decisions.expire(disruption.getId(), disruption.getDecisionDeadline().plusHours(1));
        assertThat(disruptions.findById(disruption.getId()).orElseThrow().getStatus())
                .isEqualTo("RESCHEDULED");
        assertThat(refunds.findBySubOrderId(subId)).isEmpty();
        assertThat(codes.findByReplacementSubOrderId(subId)).isEmpty();
        assertThat(subOrderRepository.findById(subId).orElseThrow().getStatus())
                .isEqualTo(SubOrderStatus.CONFIRMED);
    }

    @Test
    void existingPreviewAndRefundApiPreserveVerifiedFullRefundWithin24Hours() {
        var departure = OffsetDateTime.now(ZoneOffset.ofHours(7)).plusHours(20);
        slotA1TomorrowMorning.setDate(departure.toLocalDate());
        slotA1TomorrowMorning.setStartTime(departure.toLocalTime().withNano(0));
        serviceSlotRepository.saveAndFlush(slotA1TomorrowMorning);
        record(RefundReason.SYSTEM_FAULT);
        var quote =
                cancellationPreview.execute(
                        new GetCancellationPreviewQuery(
                                customerUser.getId(), paidOrder.getId(), false));
        assertThat(quote.refundAmount()).isEqualByComparingTo("90000");
        var command =
                new RequestRefundCommand(
                        customerUser.getId(),
                        paidOrder.getId(),
                        RefundReason.CUSTOMER_REQUEST,
                        "existing-refund-api-123",
                        null);
        var first = refundRequest.execute(command);
        var repeated = refundRequest.execute(command);
        assertThat(repeated.getFirst().refundId()).isEqualTo(first.getFirst().refundId());
        assertThat(first.getFirst().reason()).isEqualTo(RefundReason.SYSTEM_FAULT);
        assertThat(refunds.findBySubOrderId(subId)).hasSize(1);
        assertThat(codes.findByReplacementSubOrderId(subId)).isPresent();
    }

    @Test
    void zeroPaidItemRestoresOnlyDiscountBenefitWithoutSendingAZeroMoneyRefund() {
        paidSubOrder.setFinalAmount(BigDecimal.ZERO);
        paidSubOrder.setDiscountAmount(BigDecimal.valueOf(100000));
        subOrderRepository.saveAndFlush(paidSubOrder);
        record(RefundReason.SYSTEM_FAULT);
        decisions.chooseRefund(subId, customerUser.getId());
        assertThat(refunds.findBySubOrderId(subId)).isEmpty();
        assertThat(codes.findByReplacementSubOrderId(subId).orElseThrow().getDiscountValue())
                .isEqualByComparingTo("100000");
    }

    @Test
    void checkInAfterVerificationBlocksCustomerRefundAndReplacement() {
        record(RefundReason.SYSTEM_FAULT);
        paidSubOrder.setCheckedInAt(OffsetDateTime.now());
        subOrderRepository.saveAndFlush(paidSubOrder);
        assertThatThrownBy(() -> decisions.chooseRefund(subId, customerUser.getId()))
                .hasMessageContaining("without check-in");
        assertThat(refunds.findBySubOrderId(subId)).isEmpty();
        assertThat(codes.findByReplacementSubOrderId(subId)).isEmpty();
    }

    @Test
    void failedPreviousGatewayOperationRequiresReconciliationInsteadOfAnotherRefund() {
        var refund = new RefundJpaEntity();
        refund.setSubOrderId(subId);
        refund.setAmount(BigDecimal.valueOf(90000));
        refund.setReason(RefundReason.CUSTOMER_REQUEST);
        refund.setStatus(RefundStatus.FAILED);
        refunds.saveAndFlush(refund);
        assertThatThrownBy(() -> record(RefundReason.SYSTEM_FAULT))
                .hasMessageContaining("refund already exists");
        assertThat(refunds.findBySubOrderId(subId)).hasSize(1);
        assertThat(codes.findByReplacementSubOrderId(subId)).isEmpty();
    }

    @Test
    void recoveredVerifiedWeatherClosesAutomaticDecisionWithoutCancellingTrip() {
        var alert = new SafetyRuleEvaluationJpaEntity();
        alert.setSlotId(slotA1TomorrowMorning.getId());
        alert.setServiceId(serviceA1.getId());
        alert.setStatus("AWAITING_ADMIN_RESOLUTION");
        alert.setAlertLevel("RED");
        alert.setIsSafe(false);
        alert.setEvaluatedAt(OffsetDateTime.now());
        alert = weather.saveAndFlush(alert);
        decisions.recordWeather(subId, alert.getSlotId(), alert.getId());
        var disruption =
                disruptions
                        .findBySubOrderIdAndStatus(subId, DisruptionDecisionService.AWAITING)
                        .orElseThrow();
        alert.setStatus("RESOLVED_SAFE");
        alert.setAlertLevel("GREEN");
        alert.setIsSafe(true);
        weather.saveAndFlush(alert);
        decisions.expire(disruption.getId(), disruption.getDecisionDeadline());
        assertThat(disruptions.findById(disruption.getId()).orElseThrow().getStatus())
                .isEqualTo("CLOSED");
        assertThat(refunds.findBySubOrderId(subId)).isEmpty();
    }

    @Test
    void rescheduleAndAutomaticRefundRaceHasOnlyOneFinancialOutcome() throws Exception {
        var disruption = record(RefundReason.SYSTEM_FAULT);
        var latch = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first =
                    pool.submit(
                            () -> {
                                latch.await();
                                authenticateCustomer();
                                try {
                                    reschedule.execute(
                                            subId,
                                            new ConfirmRescheduleRequest(
                                                    slotA1TomorrowAfternoon.getId(), null, 0L),
                                            "race-reschedule-123");
                                } catch (ResponseStatusException expected) {
                                    assertThat(expected.getStatusCode().value()).isEqualTo(409);
                                } finally {
                                    SecurityContextHolder.clearContext();
                                }
                                return true;
                            });
            var second =
                    pool.submit(
                            () -> {
                                latch.await();
                                decisions.expire(
                                        disruption.getId(), disruption.getDecisionDeadline());
                                return true;
                            });
            latch.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(15, TimeUnit.SECONDS)).isTrue();
        }
        var finalDecision = disruptions.findById(disruption.getId()).orElseThrow();
        assertThat(finalDecision.getStatus()).isIn("RESCHEDULED", "REFUND_REQUESTED");
        assertThat(refunds.findBySubOrderId(subId))
                .hasSize("RESCHEDULED".equals(finalDecision.getStatus()) ? 0 : 1);
        assertThat(codes.findByReplacementSubOrderId(subId).isPresent())
                .isEqualTo("REFUND_REQUESTED".equals(finalDecision.getStatus()));
    }

    @Test
    void weatherWorkerRecordsDecisionBeforeDeadlineAndDoesNotGiveReplacement() {
        var alert = new SafetyRuleEvaluationJpaEntity();
        alert.setSlotId(slotA1TomorrowMorning.getId());
        alert.setServiceId(serviceA1.getId());
        alert.setStatus("AWAITING_ADMIN_RESOLUTION");
        alert.setAlertLevel("RED");
        alert.setIsSafe(false);
        alert.setEvaluatedAt(OffsetDateTime.now());
        weather.saveAndFlush(alert);
        decisionJob.expireDecisions();
        var disruption =
                disruptions
                        .findBySubOrderIdAndStatus(subId, DisruptionDecisionService.AWAITING)
                        .orElseThrow();
        assertThat(disruption.getReason()).isEqualTo(RefundReason.WEATHER);
        assertThat(refunds.findBySubOrderId(subId)).isEmpty();
        decisions.expire(disruption.getId(), disruption.getDecisionDeadline());
        assertThat(refunds.findBySubOrderId(subId)).hasSize(1);
        assertThat(codes.findByReplacementSubOrderId(subId)).isEmpty();
    }

    @Test
    void weatherCancellationPreservesAnAlreadyVerifiedSystemFaultAndItsReplacement() {
        record(RefundReason.SYSTEM_FAULT);
        assertThat(
                        weatherCancellation.cancel(
                                subId, slotA1TomorrowMorning.getId(), UUID.randomUUID()))
                .isTrue();
        assertThat(refunds.findBySubOrderId(subId).getFirst().getReason())
                .isEqualTo(RefundReason.SYSTEM_FAULT);
        assertThat(codes.findByReplacementSubOrderId(subId)).isPresent();
        assertThat(
                        weatherCancellation.cancel(
                                subId, slotA1TomorrowMorning.getId(), UUID.randomUUID()))
                .isFalse();
        assertThat(refunds.findBySubOrderId(subId)).hasSize(1);
    }

    @Test
    void cancellingOneItemDoesNotRepriceOrReplaceTheOtherItemsDiscount() {
        var remaining = new SubOrderJpaEntity();
        remaining.setMasterOrderId(paidOrder.getId());
        remaining.setVendorId(vendorProfileA.getId());
        remaining.setServiceId(serviceA1.getId());
        remaining.setSlotId(slotA1TomorrowAfternoon.getId());
        remaining.setQuantity(1);
        remaining.setStatus(SubOrderStatus.CONFIRMED);
        remaining.setSubtotalAmount(BigDecimal.valueOf(32000));
        remaining.setDiscountAmount(BigDecimal.valueOf(2000));
        remaining.setFinalAmount(BigDecimal.valueOf(30000));
        remaining = subOrderRepository.saveAndFlush(remaining);
        record(RefundReason.SYSTEM_FAULT);
        decisions.chooseRefund(subId, customerUser.getId());
        var unchanged = subOrderRepository.findById(remaining.getId()).orElseThrow();
        assertThat(unchanged.getFinalAmount()).isEqualByComparingTo("30000");
        assertThat(unchanged.getDiscountAmount()).isEqualByComparingTo("2000");
        assertThat(unchanged.getStatus()).isEqualTo(SubOrderStatus.CONFIRMED);
        assertThat(codes.findByReplacementSubOrderId(subId).orElseThrow().getDiscountValue())
                .isEqualByComparingTo("10000");
        assertThat(codes.findByReplacementSubOrderId(remaining.getId())).isEmpty();
    }

    private SubOrderDisruptionJpaEntity record(RefundReason reason) {
        return decisions.record(
                subId,
                reason,
                "Verified service interruption",
                "test-disruption-123",
                adminUser.getId());
    }

    private void authenticateCustomer() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                customerUser.getId().toString(),
                                "n/a",
                                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private UUID newHold(UUID customer) {
        var booking = new BookingJpaEntity();
        booking.setCustomerId(customer);
        booking.setStatus(BookingStatus.HOLD);
        booking.setTotalAmount(BigDecimal.valueOf(100000));
        booking.setHoldExpiresAt(OffsetDateTime.now().plusMinutes(10));
        booking = bookings.saveAndFlush(booking);
        items.saveAndFlush(bookingItem(booking, slotA1TomorrowAfternoon.getId()));
        return booking.getId();
    }

    private BookingItemJpaEntity bookingItem(BookingJpaEntity booking, UUID slot) {
        var timetable = serviceSlotRepository.findById(slot).orElseThrow();
        return BookingItemJpaEntity.builder()
                .booking(booking)
                .slotId(slot)
                .serviceId(serviceA1.getId())
                .vendorId(vendorProfileA.getId())
                .quantity(1)
                .pricingUnit("PER_PERSON")
                .bookingDate(timetable.getDate())
                .bookingTime(timetable.getStartTime())
                .price(BigDecimal.valueOf(100000))
                .build();
    }
}

package com.danasea.backend.modules.order.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.danasea.backend.e2e.BaseE2ETest;
import com.danasea.backend.modules.booking.domain.models.*;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.*;
import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.*;
import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.order.application.services.RescheduleNotificationService;
import com.danasea.backend.modules.order.domain.models.*;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.*;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.*;
import com.danasea.backend.modules.order.presentation.dtos.ConfirmRescheduleRequest;
import com.danasea.backend.modules.service.domain.models.*;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.*;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.*;
import com.danasea.backend.modules.weather.application.services.WeatherBookingCancellationService;
import com.danasea.backend.modules.weather.application.usecases.CheckAdvanceBookingSafetyUseCase;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
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
class ReschedulePersistenceIntegrationTest extends BaseE2ETest {
    @MockitoBean LettuceBasedProxyManager<byte[]> proxyManager;
    @MockitoBean CheckAdvanceBookingSafetyUseCase advanceSafety;
    @MockitoSpyBean SendNotificationUseCase notifications;
    @Autowired RescheduleNotificationService delivery;
    @Autowired ConfirmSubOrderRescheduleUseCase confirm;
    @Autowired GetRescheduleOptionsUseCase options;
    @Autowired InventoryLockPort inventory;
    @Autowired SubOrderRepositoryPort subOrderPort;
    @Autowired JpaBookingRepository bookings;
    @Autowired JpaBookingItemRepository items;
    @Autowired JpaServiceSlotUnitRepository units;
    @Autowired JpaSafetyRuleEvaluationRepository evaluations;
    @Autowired JpaSubOrderRescheduleHistoryRepository histories;
    @Autowired JpaRefundRepository refunds;
    @Autowired WeatherBookingCancellationService cancellations;
    @Autowired StringRedisTemplate redis;
    @Autowired JdbcTemplate jdbc;
    UUID oldId, targetId, subId, itemId, alertId;
    UUID holdId;
    List<InventoryLockItem> held;

    @BeforeEach
    void prepare() {
        var departure = OffsetDateTime.now(ZoneOffset.ofHours(7)).plusDays(3);
        oldId = slotA1TomorrowMorning.getId();
        targetId = slotA1TomorrowAfternoon.getId();
        subId = paidSubOrder.getId();
        slotA1TomorrowMorning.setDate(departure.toLocalDate());
        slotA1TomorrowMorning.setStartTime(departure.toLocalTime().withNano(0));
        slotA1TomorrowMorning.setEndTime(departure.plusHours(2).toLocalTime().withNano(0));
        slotA1TomorrowMorning.setBookedCount(4);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowMorning);
        slotA1TomorrowAfternoon.setDate(departure.plusDays(1).toLocalDate());
        slotA1TomorrowAfternoon.setStartTime(departure.toLocalTime().withNano(0));
        slotA1TomorrowAfternoon.setEndTime(departure.plusHours(2).toLocalTime().withNano(0));
        slotA1TomorrowAfternoon.setBookedCount(0);
        slotA1TomorrowAfternoon.setCapacity(10);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowAfternoon);
        var booking = new BookingJpaEntity();
        booking.setCustomerId(customerUser.getId());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(BigDecimal.valueOf(400000));
        booking = bookings.saveAndFlush(booking);
        var item =
                BookingItemJpaEntity.builder()
                        .booking(booking)
                        .slotId(oldId)
                        .serviceId(serviceA1.getId())
                        .vendorId(vendorProfileA.getId())
                        .quantity(4)
                        .pricingUnit("PER_PERSON")
                        .bookingDate(slotA1TomorrowMorning.getDate())
                        .bookingTime(slotA1TomorrowMorning.getStartTime())
                        .price(BigDecimal.valueOf(100000))
                        .build();
        itemId = items.saveAndFlush(item).getId();
        paidOrder.setBookingId(booking.getId());
        masterOrderRepository.saveAndFlush(paidOrder);
        paidSubOrder.setBookingItemId(itemId);
        paidSubOrder.setQuantity(4);
        paidSubOrder.setSlotId(oldId);
        subOrderRepository.saveAndFlush(paidSubOrder);
        var alert = new SafetyRuleEvaluationJpaEntity();
        alert.setSlotId(oldId);
        alert.setServiceId(serviceA1.getId());
        alert.setIsSafe(false);
        alert.setAlertLevel("RED");
        alert.setStatus("AWAITING_ADMIN_RESOLUTION");
        alert.setEvaluatedAt(OffsetDateTime.now());
        alert.setWarningMessage("Unsafe weather");
        alertId = evaluations.saveAndFlush(alert).getId();
        authenticate(customerUser.getId());
        redis.delete("inventory:slot:" + oldId + ":holds");
        redis.delete("inventory:slot:" + targetId + ":holds");
    }

    @AfterEach
    void cleanupHolds() {
        if (holdId != null) inventory.releaseHolds(holdId, held);
        SecurityContextHolder.clearContext();
    }

    void authenticate(UUID id) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                id.toString(),
                                "n/a",
                                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    void unit(UUID slot, int number, int booked) {
        var u = new ServiceSlotUnitJpaEntity();
        u.setSlotId(slot);
        u.setUnitNumber(number);
        u.setCapacity(10);
        u.setBookedCount(booked);
        units.saveAndFlush(u);
    }

    void shared() {
        slotA1TomorrowMorning.setInventoryType(InventoryType.SHARED_CAPACITY_UNITS);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowMorning);
        slotA1TomorrowAfternoon.setInventoryType(InventoryType.SHARED_CAPACITY_UNITS);
        slotA1TomorrowAfternoon.setCapacity(20);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowAfternoon);
        unit(oldId, 1, 4);
        unit(targetId, 1, 0);
        unit(targetId, 2, 0);
        var item = items.findById(itemId).orElseThrow();
        item.addAllocation(
                BookingItemAllocationJpaEntity.builder()
                        .slotId(oldId)
                        .unitNumber(1)
                        .allocatedSeats(4)
                        .isPrivateLock(false)
                        .build());
        items.saveAndFlush(item);
    }

    @Test
    void committedAllocationMatchesRedisHoldAndReplaysOnce() {
        shared();
        holdId = UUID.randomUUID();
        held =
                List.of(
                        InventoryLockItem.builder()
                                .slotId(targetId)
                                .quantity(8)
                                .maxCapacity(20)
                                .inventoryType(InventoryType.SHARED_CAPACITY_UNITS)
                                .optionType(OptionType.SHARED)
                                .allowSplit(false)
                                .units(
                                        List.of(
                                                new InventoryLockItem.UnitLockInfo(1, 10, 0),
                                                new InventoryLockItem.UnitLockInfo(2, 10, 0)))
                                .build());
        inventory.acquireHolds(holdId, held, Duration.ofMinutes(5));
        assertEquals(1, held.getFirst().getAllocations().getFirst().getUnitNumber());
        var request = new ConfirmRescheduleRequest(targetId, null, 0L);
        var result = confirm.execute(subId, request, "real-reschedule-1");
        assertEquals(
                2,
                items.findById(itemId).orElseThrow().getAllocations().getFirst().getUnitNumber());
        assertEquals(4, serviceSlotRepository.findById(targetId).orElseThrow().getBookedCount());
        assertEquals(0, serviceSlotRepository.findById(oldId).orElseThrow().getBookedCount());
        assertEquals(result, confirm.execute(subId, request, "real-reschedule-1"));
        assertEquals(1, histories.findBySubOrderIdOrderByCreatedAtDesc(subId).size());
        assertNotNull(
                histories
                        .findBySubOrderIdOrderByCreatedAtDesc(subId)
                        .getFirst()
                        .getNotificationSentAt());
        var domain = subOrderPort.findById(subId).orElseThrow();
        assertEquals(1L, domain.getRescheduleVersion());
        domain.setVendorNotifiedAt(OffsetDateTime.now());
        subOrderPort.save(domain);
        var persisted = subOrderRepository.findById(subId).orElseThrow();
        assertEquals(1L, persisted.getRescheduleVersion());
        assertEquals(oldId, persisted.getOriginalSlotId());
        assertNotNull(persisted.getRescheduledAt());
    }

    @Test
    void personLimitHoldIncludesExistingBookedAndHeldCapacity() {
        slotA1TomorrowAfternoon.setBookedCount(6);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowAfternoon);
        holdId = UUID.randomUUID();
        held = List.of(InventoryLockItem.of(targetId, 1, 4));
        inventory.acquireHolds(holdId, held, Duration.ofMinutes(5));
        assertThrows(
                RuntimeException.class,
                () ->
                        confirm.execute(
                                subId,
                                new ConfirmRescheduleRequest(targetId, null, 0L),
                                "capacity-test-1"));
        assertEquals(oldId, subOrderRepository.findById(subId).orElseThrow().getSlotId());
        assertEquals(6, serviceSlotRepository.findById(targetId).orElseThrow().getBookedCount());
        var result =
                options.execute(subId).options().stream()
                        .filter(s -> s.slotId().equals(targetId))
                        .findFirst()
                        .orElseThrow();
        assertEquals(3, result.availableCapacity());
        assertFalse(result.canAccommodate());
    }

    @Test
    void rescheduleAndWeatherCancellationSerializeWithoutCorruptingInventory() throws Exception {
        var start = new CountDownLatch(1);
        UUID userId = customerUser.getId();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var moving =
                    executor.submit(
                            () -> {
                                start.await();
                                authenticate(userId);
                                try {
                                    confirm.execute(
                                            subId,
                                            new ConfirmRescheduleRequest(targetId, null, 0L),
                                            "race-reschedule-1");
                                    return true;
                                } catch (RuntimeException ex) {
                                    return false;
                                } finally {
                                    SecurityContextHolder.clearContext();
                                }
                            });
            var cancelling =
                    executor.submit(
                            () -> {
                                start.await();
                                return cancellations.cancel(subId, oldId, alertId);
                            });
            start.countDown();
            boolean moved = moving.get(30, TimeUnit.SECONDS);
            boolean cancelled = cancelling.get(30, TimeUnit.SECONDS);
            assertNotEquals(moved, cancelled);
            var sub = subOrderRepository.findById(subId).orElseThrow();
            assertEquals(0, serviceSlotRepository.findById(oldId).orElseThrow().getBookedCount());
            assertEquals(
                    moved ? 4 : 0,
                    serviceSlotRepository.findById(targetId).orElseThrow().getBookedCount());
            assertEquals(moved ? targetId : oldId, sub.getSlotId());
            assertEquals(
                    moved ? SubOrderStatus.CONFIRMED : SubOrderStatus.CANCELLED, sub.getStatus());
            assertEquals(cancelled ? 1 : 0, refunds.findAll().size());
            if (cancelled)
                assertEquals(RefundStatus.PENDING, refunds.findAll().getFirst().getStatus());
        }
    }

    @Test
    @Transactional
    void cleanupRemovesRescheduleHistoryWithoutDatabaseForeignKeys() {
        confirm.execute(
                subId,
                new ConfirmRescheduleRequest(targetId, null, 0L),
                "cleanup-history-1");
        assertEquals(1, histories.findBySubOrderIdOrderByCreatedAtDesc(subId).size());

        // Hibernate-created test schemas may omit FK relationships represented as UUID fields.
        // This transaction rolls the constraint changes back after verifying fixture cleanup.
        var constraints =
                jdbc.queryForList(
                        "SELECT conname FROM pg_constraint WHERE"
                                + " conrelid = 'public.sub_order_reschedule_history'::regclass"
                                + " AND contype = 'f'",
                        String.class);
        for (String constraint : constraints) {
            jdbc.execute(
                    "ALTER TABLE sub_order_reschedule_history DROP CONSTRAINT \""
                            + constraint.replace("\"", "\"\"")
                            + "\"");
        }

        cleanDatabase();

        assertEquals(0, histories.count());
    }

    @Test
    void twoOrdersCompetingForLastCapacityHaveOneWinner() throws Exception {
        slotA1TomorrowMorning.setBookedCount(8);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowMorning);
        slotA1TomorrowAfternoon.setCapacity(4);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowAfternoon);
        var booking = new BookingJpaEntity();
        booking.setCustomerId(customerUser.getId());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(BigDecimal.valueOf(400000));
        booking = bookings.saveAndFlush(booking);
        var secondItem =
                BookingItemJpaEntity.builder()
                        .booking(booking)
                        .slotId(oldId)
                        .serviceId(serviceA1.getId())
                        .vendorId(vendorProfileA.getId())
                        .quantity(4)
                        .pricingUnit("PER_PERSON")
                        .bookingDate(slotA1TomorrowMorning.getDate())
                        .bookingTime(slotA1TomorrowMorning.getStartTime())
                        .price(BigDecimal.valueOf(100000))
                        .build();
        secondItem = items.saveAndFlush(secondItem);
        cancelledOrder.setStatus(MasterOrderStatus.PAID);
        cancelledOrder.setPaymentStatus(PaymentOrderStatus.PAID);
        cancelledOrder.setBookingId(booking.getId());
        masterOrderRepository.saveAndFlush(cancelledOrder);
        cancelledSubOrder.setStatus(SubOrderStatus.CONFIRMED);
        cancelledSubOrder.setSlotId(oldId);
        cancelledSubOrder.setServiceId(serviceA1.getId());
        cancelledSubOrder.setVendorId(vendorProfileA.getId());
        cancelledSubOrder.setQuantity(4);
        cancelledSubOrder.setBookingItemId(secondItem.getId());
        subOrderRepository.saveAndFlush(cancelledSubOrder);
        UUID secondId = cancelledSubOrder.getId(), userId = customerUser.getId();
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a =
                    pool.submit(
                            () -> {
                                start.await();
                                return attempt(subId, userId, "last-capacity-1");
                            });
            var b =
                    pool.submit(
                            () -> {
                                start.await();
                                return attempt(secondId, userId, "last-capacity-2");
                            });
            start.countDown();
            boolean firstMoved = a.get(30, TimeUnit.SECONDS);
            boolean secondMoved = b.get(30, TimeUnit.SECONDS);
            assertNotEquals(firstMoved, secondMoved);
            assertEquals(
                    4, serviceSlotRepository.findById(targetId).orElseThrow().getBookedCount());
            assertEquals(4, serviceSlotRepository.findById(oldId).orElseThrow().getBookedCount());
            assertEquals(
                    firstMoved ? 1 : 0,
                    histories.findBySubOrderIdOrderByCreatedAtDesc(subId).size());
            assertEquals(
                    secondMoved ? 1 : 0,
                    histories.findBySubOrderIdOrderByCreatedAtDesc(secondId).size());
        }
    }

    boolean attempt(UUID id, UUID userId, String key) {
        authenticate(userId);
        try {
            confirm.execute(id, new ConfirmRescheduleRequest(targetId, null, 0L), key);
            return true;
        } catch (ResponseStatusException conflict) {
            assertEquals(409, conflict.getStatusCode().value());
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void packageSnapshotPreventsMovingEightGuestsToFourCapacityUnit() {
        shared();
        var oldUnit = units.findBySlotIdOrderByUnitNumberAsc(oldId).getFirst();
        oldUnit.setBookedCount(10);
        units.saveAndFlush(oldUnit);
        slotA1TomorrowMorning.setBookedCount(10);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowMorning);
        for (var unit : units.findBySlotIdOrderByUnitNumberAsc(targetId)) {
            unit.setCapacity(4);
            units.saveAndFlush(unit);
        }
        slotA1TomorrowAfternoon.setCapacity(8);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowAfternoon);
        var item = items.findById(itemId).orElseThrow();
        item.setPricingUnit("PER_PACKAGE");
        item.setQuantity(1);
        item.setParticipantsCount(8);
        item.setMaxPaxPerPackage(10);
        item.getAllocations().getFirst().setAllocatedSeats(10);
        item.getAllocations().getFirst().setIsPrivateLock(true);
        items.saveAndFlush(item);
        paidSubOrder.setQuantity(1);
        subOrderRepository.saveAndFlush(paidSubOrder);
        assertThrows(
                ResponseStatusException.class,
                () ->
                        confirm.execute(
                                subId,
                                new ConfirmRescheduleRequest(targetId, null, 0L),
                                "package-limit-1"));
        assertEquals(oldId, subOrderRepository.findById(subId).orElseThrow().getSlotId());
        assertEquals(10, serviceSlotRepository.findById(oldId).orElseThrow().getBookedCount());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void largeGroupOnlySplitsWithOriginalConsent(boolean consent) {
        shared();
        slotA1TomorrowMorning.setCapacity(20);
        slotA1TomorrowMorning.setBookedCount(12);
        serviceSlotRepository.saveAndFlush(slotA1TomorrowMorning);
        var oldUnit = units.findBySlotIdOrderByUnitNumberAsc(oldId).getFirst();
        oldUnit.setBookedCount(10);
        units.saveAndFlush(oldUnit);
        unit(oldId, 2, 2);
        var item = items.findById(itemId).orElseThrow();
        item.setQuantity(12);
        item.setAllowSplit(consent);
        item.getAllocations().getFirst().setAllocatedSeats(10);
        item.addAllocation(
                BookingItemAllocationJpaEntity.builder()
                        .slotId(oldId)
                        .unitNumber(2)
                        .allocatedSeats(2)
                        .isPrivateLock(false)
                        .build());
        items.saveAndFlush(item);
        paidSubOrder.setQuantity(12);
        subOrderRepository.saveAndFlush(paidSubOrder);
        if (!consent)
            assertThrows(
                    ResponseStatusException.class,
                    () ->
                            confirm.execute(
                                    subId,
                                    new ConfirmRescheduleRequest(targetId, null, 0L),
                                    "large-group-1"));
        else {
            confirm.execute(
                    subId, new ConfirmRescheduleRequest(targetId, null, 0L), "large-group-1");
            var moved = items.findById(itemId).orElseThrow();
            assertEquals(2, moved.getAllocations().size());
            assertEquals(
                    12,
                    moved.getAllocations().stream()
                            .mapToInt(BookingItemAllocationJpaEntity::getAllocatedSeats)
                            .sum());
            assertEquals(0, serviceSlotRepository.findById(oldId).orElseThrow().getBookedCount());
        }
    }

    @Test
    void failedAfterCommitNotificationRemainsDurableAndRetriesExactlyOnce() {
        Mockito.doThrow(new IllegalStateException("Notification DB unavailable"))
                .when(notifications)
                .executeOnce(ArgumentMatchers.any(), ArgumentMatchers.anyString());
        confirm.execute(
                subId, new ConfirmRescheduleRequest(targetId, null, 0L), "notification-retry-1");
        var history = histories.findBySubOrderIdOrderByCreatedAtDesc(subId).getFirst();
        assertNull(history.getNotificationSentAt());
        assertEquals(targetId, subOrderRepository.findById(subId).orElseThrow().getSlotId());
        Mockito.doCallRealMethod()
                .when(notifications)
                .executeOnce(ArgumentMatchers.any(), ArgumentMatchers.anyString());
        long count = notificationRepository.count();
        delivery.completed(history.getId());
        delivery.completed(history.getId());
        assertNotNull(histories.findById(history.getId()).orElseThrow().getNotificationSentAt());
        assertEquals(count + 2, notificationRepository.count());
    }

    @Test
    void attackerAndAdminCannotConfirmCustomerRescheduleOverHttp() throws Exception {
        String payload = "{\"targetSlotId\":\"" + targetId + "\",\"expectedVersion\":0}";
        SecurityContextHolder.clearContext();
        TestSecurityContextHolder.clearContext();
        for (String token : List.of(attackerToken, adminToken))
            mockMvc.perform(
                            post("/api/sub-orders/" + subId + "/reschedule")
                                    .header("Authorization", "Bearer " + token)
                                    .header("Idempotency-Key", "http-reschedule-1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                    .andExpect(status().isForbidden());
    }
}

package com.danasea.backend.modules.order.application.usecases;

import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.JpaBookingItemRepository;
import com.danasea.backend.modules.order.application.services.DisruptionDecisionService;
import com.danasea.backend.modules.order.application.services.RescheduleSupport;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderRescheduleProposalJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderDisruptionRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRescheduleProposalRepository;
import com.danasea.backend.modules.order.presentation.dtos.RescheduleOptionItemResponse;
import com.danasea.backend.modules.order.presentation.dtos.RescheduleOptionsSummaryResponse;
import com.danasea.backend.modules.service.application.services.SlotAvailabilityCalculator;
import com.danasea.backend.modules.service.domain.models.OptionType;
import com.danasea.backend.modules.service.domain.models.ServiceOption;
import com.danasea.backend.modules.service.domain.models.ServiceSlot;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.models.SlotStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import com.danasea.backend.shared.i18n.LocalizedMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetRescheduleOptionsUseCase {

    private final JpaSubOrderRepository subOrderRepository;
    private final JpaMasterOrderRepository masterOrderRepository;
    private final JpaServiceSlotRepository slotRepository;
    private final JpaServiceRepository serviceRepository;
    private final JpaBookingItemRepository bookingItemRepository;
    private final JpaSubOrderRescheduleProposalRepository proposalRepository;
    private final JpaSafetyRuleEvaluationRepository evaluationRepository;

    private final SlotAvailabilityCalculator availabilityCalculator;
    private final JpaSubOrderDisruptionRepository disruptions;

    @Transactional(readOnly = true)
    public RescheduleOptionsSummaryResponse execute(UUID subOrderId) {
        UUID currentUserId =
                SecurityUtils.getCurrentUserId()
                        .orElseThrow(() -> new AccessDeniedException("User is not authenticated."));

        SubOrderJpaEntity subOrder =
                subOrderRepository
                        .findById(subOrderId)
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                "Sub-order not found with id: " + subOrderId));

        MasterOrderJpaEntity masterOrder =
                masterOrderRepository
                        .findById(subOrder.getMasterOrderId())
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                "Master order not found with id: "
                                                        + subOrder.getMasterOrderId()));

        // Chống IDOR: Chỉ chủ đơn hoặc Admin
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin =
                auth != null
                        && auth.getAuthorities().stream()
                                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !Objects.equals(masterOrder.getCustomerId(), currentUserId)) {
            throw new AccessDeniedException("User does not own this order.");
        }

        RescheduleSupport.eligible(masterOrder, subOrder);
        var currentSlot =
                slotRepository
                        .findById(subOrder.getSlotId())
                        .orElseThrow(() -> RescheduleSupport.conflict("Current slot not found."));
        if (!RescheduleSupport.upcoming(currentSlot))
            throw RescheduleSupport.conflict("Current slot has already started.");
        var service =
                serviceRepository
                        .findById(subOrder.getServiceId())
                        .orElseThrow(() -> RescheduleSupport.conflict("Service not found."));
        var item =
                bookingItemRepository
                        .findById(subOrder.getBookingItemId())
                        .orElseThrow(() -> RescheduleSupport.conflict("Booking item not found."));
        boolean weather =
                evaluationRepository
                        .findTopBySlotIdOrderByEvaluatedAtDesc(currentSlot.getId())
                        .map(RescheduleSupport::weatherAlert)
                        .orElse(false);
        var proposals =
                proposalRepository.findBySubOrderIdAndStatusAndExpiresAtAfter(
                        subOrderId, "PENDING", OffsetDateTime.now());
        var disruption =
                disruptions.findBySubOrderIdAndStatus(
                        subOrderId, DisruptionDecisionService.AWAITING);
        var deadline =
                disruption
                        .map(d -> d.getDecisionDeadline())
                        .orElseGet(
                                () ->
                                        currentSlot
                                                .getDate()
                                                .atTime(currentSlot.getStartTime())
                                                .atOffset(ZoneOffset.ofHours(7))
                                                .minusHours(12));
        boolean beforeDeadline = deadline.isAfter(OffsetDateTime.now());
        boolean refundAvailable =
                disruption.isPresent()
                        || evaluationRepository
                                .findTopBySlotIdOrderByEvaluatedAtDesc(currentSlot.getId())
                                .filter(
                                        a ->
                                                RescheduleSupport.weatherAlert(a)
                                                        && (Boolean.FALSE.equals(a.getIsSafe())
                                                                || "RED".equals(a.getAlertLevel())))
                                .isPresent();
        boolean decisionRequired = refundAvailable || !proposals.isEmpty();
        boolean rescheduleWindowOpen = !decisionRequired || beforeDeadline;
        boolean privatePackage = "PER_PACKAGE".equals(item.getPricingUnit());
        var option =
                ServiceOption.builder()
                        .optionType(privatePackage ? OptionType.PRIVATE : OptionType.SHARED)
                        .maxPaxPerPackage(RescheduleSupport.maxPax(item))
                        .build();
        List<RescheduleOptionItemResponse> options = new ArrayList<>();
        if (service.getStatus() == ServiceStatus.PUBLISHED) {
            LocalDate from = LocalDate.now(Booking.VIETNAM_ZONE);
            for (var slot :
                    slotRepository.findByServiceIdAndDateBetweenOrderByDateAscStartTimeAsc(
                            service.getId(), from, from.plusDays(14))) {
                if (slot.getId().equals(currentSlot.getId())
                        || slot.getStatus() != SlotStatus.OPEN
                        || !RescheduleSupport.upcoming(slot)) continue;
                var alert =
                        evaluationRepository.findTopBySlotIdOrderByEvaluatedAtDesc(slot.getId());
                if (alert.filter(
                                a ->
                                        Boolean.FALSE.equals(a.getIsSafe())
                                                || "RED".equals(a.getAlertLevel()))
                        .isPresent()) continue;
                var domain =
                        ServiceSlot.builder()
                                .id(slot.getId())
                                .serviceId(slot.getServiceId())
                                .date(slot.getDate())
                                .startTime(slot.getStartTime())
                                .capacity(slot.getCapacity())
                                .bookedCount(slot.getBookedCount())
                                .inventoryType(slot.getInventoryType())
                                .status(slot.getStatus())
                                .build();
                var remaining = availabilityCalculator.evaluate(domain, option);
                var proposal =
                        proposals.stream()
                                .filter(p -> slot.getId().equals(p.getProposedSlotId()))
                                .findFirst();
                boolean allowed =
                        rescheduleWindowOpen
                                && (weather || disruption.isPresent() || proposal.isPresent());
                boolean fits =
                        remaining.canBook(
                                item.getQuantity(),
                                !privatePackage,
                                item.isAllowSplit(),
                                slot.getInventoryType());
                options.add(
                        new RescheduleOptionItemResponse(
                                slot.getId(),
                                slot.getDate(),
                                slot.getStartTime(),
                                slot.getEndTime(),
                                remaining.available(),
                                fits && allowed,
                                LocalizedMessageService.standalone()
                                        .get(
                                                !allowed
                                                        ? "reschedule.no_proposal"
                                                        : (fits
                                                                ? "reschedule.available"
                                                                : "reschedule.insufficient_capacity")),
                                item.getPrice(),
                                BigDecimal.ZERO,
                                proposal.isPresent(),
                                proposal.map(SubOrderRescheduleProposalJpaEntity::getId)
                                        .orElse(null),
                                proposal.map(SubOrderRescheduleProposalJpaEntity::getExpiresAt)
                                        .orElse(null)));
            }
        }
        return new RescheduleOptionsSummaryResponse(
                subOrderId,
                currentSlot.getId(),
                currentSlot.getDate(),
                currentSlot.getStartTime(),
                service.getId(),
                service.getName(),
                item.getQuantity(),
                item.getPricingUnit(),
                subOrder.getRescheduleVersion(),
                rescheduleWindowOpen && (weather || disruption.isPresent() || !proposals.isEmpty()),
                weather
                        ? "WEATHER_ALERT"
                        : disruption
                                .map(d -> d.getReason().name())
                                .orElse(proposals.isEmpty() ? null : "VENDOR_PROPOSAL"),
                options,
                decisionRequired ? deadline : null,
                refundAvailable,
                disruption
                        .map(d -> d.getReason())
                        .orElse(refundAvailable ? RefundReason.WEATHER : null));
    }
}

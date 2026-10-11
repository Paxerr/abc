package com.danasea.backend.modules.order.infrastructure.jobs;

import com.danasea.backend.modules.order.application.services.DisruptionDecisionService;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderDisruptionRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DisruptionDecisionJob {
    private final JpaSubOrderDisruptionRepository disruptions;
    private final DisruptionDecisionService decisions;
    private final JpaSafetyRuleEvaluationRepository weather;
    private final JpaSubOrderRepository subOrders;

    @Scheduled(fixedDelayString = "${app.scheduler.disruption-decision-delay:30s}")
    public void expireDecisions() {
        for (var alert :
                weather.findTop100ByStatusOrderByEvaluatedAtDesc("AWAITING_ADMIN_RESOLUTION")) {
            if (alert.getSlotId() == null
                    || (!Boolean.FALSE.equals(alert.getIsSafe())
                            && !"RED".equals(alert.getAlertLevel()))) continue;
            for (var sub : subOrders.findBySlotId(alert.getSlotId())) {
                try {
                    decisions.recordWeather(sub.getId(), alert.getSlotId(), alert.getId());
                } catch (RuntimeException ex) {
                    log.error(
                            "Could not record weather disruption for {}: {}",
                            sub.getId(),
                            ex.getMessage(),
                            ex);
                }
            }
        }
        var now = OffsetDateTime.now();
        for (var disruption :
                disruptions
                        .findTop100ByStatusAndDecisionDeadlineLessThanEqualOrderByDecisionDeadlineAsc(
                                DisruptionDecisionService.AWAITING, now)) {
            try {
                decisions.expire(disruption.getId(), now);
            } catch (RuntimeException ex) {
                log.error(
                        "Could not resolve disruption {}: {}",
                        disruption.getId(),
                        ex.getMessage(),
                        ex);
            }
        }
    }
}

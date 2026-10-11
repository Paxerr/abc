package com.danasea.backend.modules.weather.application.services;

import com.danasea.backend.modules.order.application.services.DisruptionDecisionService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeatherBookingCancellationService {
    private final DisruptionDecisionService decisions;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancel(UUID subOrderId, UUID expectedSlotId, UUID evaluationId) {
        return decisions.forceWeatherRefund(subOrderId, expectedSlotId, evaluationId);
    }
}

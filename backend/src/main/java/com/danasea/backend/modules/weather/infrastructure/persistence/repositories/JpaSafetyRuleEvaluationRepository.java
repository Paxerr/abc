package com.danasea.backend.modules.weather.infrastructure.persistence.repositories;

import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSafetyRuleEvaluationRepository
        extends JpaRepository<SafetyRuleEvaluationJpaEntity, UUID> {
    List<SafetyRuleEvaluationJpaEntity> findTop100ByStatusOrderByEvaluatedAtDesc(String status);

    List<SafetyRuleEvaluationJpaEntity> findByIsSafeFalse();

    List<SafetyRuleEvaluationJpaEntity> findByStatus(String status);

    List<SafetyRuleEvaluationJpaEntity> findByStatusIn(List<String> statuses);

    Optional<SafetyRuleEvaluationJpaEntity> findTopBySlotIdOrderByEvaluatedAtDesc(UUID slotId);

    Optional<SafetyRuleEvaluationJpaEntity> findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(
            UUID slotId, List<String> statuses);

    List<SafetyRuleEvaluationJpaEntity> findBySlotId(UUID slotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM SafetyRuleEvaluationJpaEntity e WHERE e.id = :id")
    Optional<SafetyRuleEvaluationJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}

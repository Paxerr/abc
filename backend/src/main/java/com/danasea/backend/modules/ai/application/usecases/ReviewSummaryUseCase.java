package com.danasea.backend.modules.ai.application.usecases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.ports.DecisionModelPort;
import com.danasea.backend.modules.ai.application.ports.ReviewHighlightPort;
import com.danasea.backend.modules.ai.application.ports.TravelDataPort;
import com.danasea.backend.modules.ai.domain.models.DecisionResult;
import com.danasea.backend.modules.ai.domain.models.DecisionTask;
import com.danasea.backend.modules.operation.application.api.AiReviewReadApi;
import com.danasea.backend.modules.operation.application.api.AiReviewReadApi.ReviewEvidence;
import com.danasea.backend.modules.operation.application.api.AiReviewReadApi.ReviewSnapshot;
import com.danasea.backend.shared.i18n.SupportedLanguage;

@Service
public class ReviewSummaryUseCase {
    public record AnalyzedReview(ReviewEvidence source, DecisionResult analysis) {}
    public record AspectEvidence(String aspect, UUID reviewId, String exactQuote, int rating, String method) {}
    public record Result(UUID serviceId, String status, long totalPublicReviews, int sampledReviews,
                         BigDecimal sampledAverageRating, long positiveRatingCount, long negativeRatingCount,
                         List<ReviewEvidence> highlights, List<ReviewEvidence> positiveExamples, List<ReviewEvidence> negativeExamples, List<AnalyzedReview> analyses,
                         String selectionMethod, boolean exhaustive, String claimScope,
                         BigDecimal publicAverageRating, Map<Integer, Long> ratingDistribution,
                         List<AspectEvidence> aspectEvidence, String sourceFingerprint, OffsetDateTime generatedAt,
                         String aggregateScope, List<String> limitations) {}
    private record CacheKey(UUID serviceId, SupportedLanguage language, String fingerprint) {}
    private static final Map<String, List<String>> ASPECT_TERMS = Map.of(
            "GUIDE", List.of("guide", "instructor", "staff", "hướng dẫn", "nhân viên"),
            "SAFETY", List.of("safe", "equipment", "life jacket", "an toàn", "áo phao", "thiết bị"),
            "PUNCTUALITY", List.of("late", "wait", "punctual", "đúng giờ", "trễ", "chờ"),
            "VALUE", List.of("price", "expensive", "value", "giá", "đáng tiền", "đắt"));
    private final TravelDataPort data;
    private final DecisionModelPort decisions;
    private final ReviewHighlightPort highlights;
    private final AiReviewReadApi reviewReads;
    private final Map<CacheKey, Result> cache = new ConcurrentHashMap<>();

    @Autowired
    public ReviewSummaryUseCase(TravelDataPort data, DecisionModelPort decisions, ReviewHighlightPort highlights, AiReviewReadApi reviewReads) {
        this.data = data; this.decisions = decisions; this.highlights = highlights; this.reviewReads = reviewReads;
    }
    public ReviewSummaryUseCase(TravelDataPort data, DecisionModelPort decisions, ReviewHighlightPort highlights) {
        this(data, decisions, highlights, null);
    }

    public Result execute(UUID serviceId, SupportedLanguage language) {
        data.metadata(serviceId, language);
        // Every lookup validates current public sources before reusing model work.
        ReviewSnapshot snapshot = reviewReads == null ? legacySnapshot(serviceId) : reviewReads.snapshot(serviceId, 50);
        CacheKey key = new CacheKey(serviceId, language, snapshot.fingerprint());
        Result cached = cache.get(key);
        if (cached != null && cached.generatedAt().plus(Duration.ofMinutes(5)).isAfter(OffsetDateTime.now())) return cached;
        Result result = summarize(serviceId, language, snapshot);
        cache.keySet().removeIf(existing -> existing.serviceId().equals(serviceId) && !existing.fingerprint().equals(key.fingerprint()));
        if (cache.size() >= 256) cache.clear();
        cache.put(key, result);
        return result;
    }

    private Result summarize(UUID serviceId, SupportedLanguage language, ReviewSnapshot snapshot) {
        List<ReviewEvidence> sources = snapshot.representatives();
        List<String> limitations = new ArrayList<>();
        if (snapshot.totalCount() < 5) limitations.add("INSUFFICIENT_REVIEWS_FOR_GENERALIZED_CONCLUSIONS");
        if (snapshot.totalCount() > sources.size()) limitations.add("ASPECTS_AND_QUOTES_COVER_A_REPRESENTATIVE_SAMPLE");
        if (reviewReads == null) limitations.add("POPULATION_AGGREGATES_UNAVAILABLE");
        if (sources.isEmpty()) return new Result(serviceId, "NO_REVIEWS", snapshot.totalCount(), 0, null, 0, 0,
                List.of(), List.of(), List.of(), List.of(), "NONE", snapshot.totalCount() == 0, "NO_REVIEW_CLAIMS",
                snapshot.averageRating(), snapshot.ratingDistribution(), List.of(), snapshot.fingerprint(), snapshot.readAt(),
                reviewReads == null ? "UNAVAILABLE" : "ALL_PUBLIC_UNFLAGGED_VALID_RATINGS", List.copyOf(limitations));
        Set<UUID> allowed = new LinkedHashSet<>(sources.stream().map(ReviewEvidence::id).toList());
        List<UUID> selected = highlights.select(sources, language);
        if (selected == null || selected.size() > 6 || !allowed.containsAll(selected)) selected = List.of();
        String method = selected.isEmpty() ? "EXTRACTIVE_BASELINE" : "CLIPROXY_SELECTED_VERIFIED_SOURCE_IDS";
        if (selected.isEmpty()) {
            var combined = new LinkedHashSet<>(sources.stream().sorted(Comparator.comparingInt(ReviewEvidence::rating))
                    .limit(3).map(ReviewEvidence::id).toList());
            sources.stream().sorted(Comparator.comparingInt(ReviewEvidence::rating).reversed())
                    .limit(3).map(ReviewEvidence::id).forEach(combined::add);
            selected = List.copyOf(combined);
        }
        List<AnalyzedReview> analyses = new ArrayList<>();
        // Keep decision work bounded; extractive evidence remains available during model failure.
        for (ReviewEvidence source : sources.stream().filter(r -> r.comment() != null && !r.comment().isBlank()).limit(8).toList()) {
            DecisionResult analysis = decisions.decide(DecisionTask.REVIEW, Map.of("review", source.comment()));
            if (analysis == null) analysis = DecisionResult.unavailable(DecisionTask.REVIEW, "MODEL_UNAVAILABLE");
            analyses.add(new AnalyzedReview(source, analysis));
        }
        List<UUID> selection = selected;
        BigDecimal average = BigDecimal.valueOf(sources.stream().mapToInt(ReviewEvidence::rating).sum())
                .divide(BigDecimal.valueOf(sources.size()), 2, RoundingMode.HALF_UP);
        List<AspectEvidence> aspects = new ArrayList<>();
        for (ReviewEvidence source : sources) {
            if (source.comment() == null || source.comment().isBlank()) continue;
            String normalized = source.comment().toLowerCase(Locale.ROOT);
            ASPECT_TERMS.entrySet().stream().sorted(Map.Entry.comparingByKey()).filter(entry -> entry.getValue().stream().anyMatch(normalized::contains))
                    .forEach(entry -> aspects.add(new AspectEvidence(entry.getKey(), source.id(), source.comment(), source.rating(), "EXACT_QUOTE_WITH_KEYWORD_TAG; TAG_IS_NOT_A_SENTIMENT_CLAIM")));
        }
        return new Result(serviceId, snapshot.totalCount() < 5 ? "LOW_EVIDENCE" : "AVAILABLE", snapshot.totalCount(), sources.size(), average,
                snapshot.positiveCount(), snapshot.negativeCount(),
                sources.stream().filter(review -> selection.contains(review.id())).toList(),
                sources.stream().filter(review -> review.rating() >= 4).limit(3).toList(),
                sources.stream().filter(review -> review.rating() <= 2).limit(3).toList(), List.copyOf(analyses),
                method, snapshot.totalCount() == sources.size(), "EXACT_REVIEW_QUOTES; MODEL_ASPECT_AND_SENTIMENT_ARE_SUGGESTIONS_ONLY",
                snapshot.averageRating(), snapshot.ratingDistribution(), List.copyOf(aspects), snapshot.fingerprint(), snapshot.readAt(),
                reviewReads == null ? "UNAVAILABLE" : "ALL_PUBLIC_UNFLAGGED_VALID_RATINGS", List.copyOf(limitations));
    }

    private ReviewSnapshot legacySnapshot(UUID serviceId) {
        List<ReviewEvidence> sources = data.reviews(serviceId, 50);
        long count = data.reviewCount(serviceId);
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            int value = rating;
            distribution.put(rating, sources.stream().filter(source -> source.rating() == value).count());
        }
        String fingerprint = Integer.toHexString(sources.hashCode()) + ":" + count;
        return new ReviewSnapshot(count, null, distribution.get(4) + distribution.get(5), distribution.get(1) + distribution.get(2),
                Map.copyOf(distribution), sources, fingerprint, OffsetDateTime.now());
    }
}

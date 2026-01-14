package ru.practicum.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityService {

    private static final int SCORE_SCALE = 2;
    private static final RoundingMode SCORE_ROUNDING = RoundingMode.HALF_UP;

    private final ConcurrentMap<Long, ConcurrentMap<Long, Double>> weightMatrix = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Double> normByEvent = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ConcurrentMap<Long, Double>> similarityMatrix = new ConcurrentHashMap<>();

    public List<EventSimilarityAvro> calculate(UserActionAvro userAction) {
        if (userAction == null) return Collections.emptyList();

        Long eventId = userAction.getEventId();
        Long userId = userAction.getUserId();
        if (eventId == null || userId == null) {
            log.debug("Skip userAction with null ids: {}", userAction);
            return Collections.emptyList();
        }

        double rating = ratingOf(userAction);
        ConcurrentMap<Long, Double> usersWeights =
                weightMatrix.computeIfAbsent(eventId, id -> new ConcurrentHashMap<>());

        boolean updated = upsertMax(usersWeights, userId, rating);
        if (!updated) {
            return Collections.emptyList();
        }

        normByEvent.put(eventId, norm(usersWeights));

        List<EventSimilarityAvro> result = calculateSimilarityForEvent(eventId, userId);

        log.debug("Updated weights for eventId={}, userId={}, rating={}, produced {} similarity msgs",
                eventId, userId, rating, result.size());

        return result;
    }

    private boolean upsertMax(ConcurrentMap<Long, Double> usersWeights, Long userId, double rating) {
        Double current = usersWeights.get(userId);
        if (current == null || rating > current) {
            usersWeights.put(userId, rating);
            return true;
        }
        return false;
    }

    private List<EventSimilarityAvro> calculateSimilarityForEvent(Long eventId, Long baseUserId) {
        Map<Long, Double> a = weightMatrix.get(eventId);
        if (a == null || a.isEmpty()) return Collections.emptyList();

        double normA = normByEvent.computeIfAbsent(eventId, id -> norm(a));
        if (normA <= 0.0) return Collections.emptyList();

        List<EventSimilarityAvro> result = new ArrayList<>();

        for (Map.Entry<Long, ConcurrentMap<Long, Double>> entry : weightMatrix.entrySet()) {
            Long otherEventId = entry.getKey();
            if (otherEventId.equals(eventId)) continue;

            Map<Long, Double> b = entry.getValue();
            if (b == null || b.isEmpty()) continue;

            if (!b.containsKey(baseUserId)) continue;

            double sumMin = overlapMinSum(a, b);
            if (sumMin <= 0.0) continue;

            double normB = normByEvent.computeIfAbsent(otherEventId, id -> norm(b));
            if (normB <= 0.0) continue;

            double similarity = sumMin / (normA * normB);
            if (similarity <= 0.0) continue;

            put(eventId, otherEventId, similarity);

            double rounded = BigDecimal.valueOf(similarity)
                    .setScale(SCORE_SCALE, SCORE_ROUNDING)
                    .doubleValue();

            long first = Math.min(eventId, otherEventId);
            long second = Math.max(eventId, otherEventId);

            EventSimilarityAvro msg = EventSimilarityAvro.newBuilder()
                    .setEventA((int) first)
                    .setEventB((int) second)
                    .setScore(rounded)
                    .setTimestamp(Instant.now())
                    .build();

            result.add(msg);
        }

        return result;
    }

    private double overlapMinSum(Map<Long, Double> a, Map<Long, Double> b) {
        Map<Long, Double> small = a.size() <= b.size() ? a : b;
        Map<Long, Double> large = small == a ? b : a;

        double sum = 0.0;
        for (Map.Entry<Long, Double> e : small.entrySet()) {
            Double other = large.get(e.getKey());
            if (other != null) {
                sum += Math.min(e.getValue(), other);
            }
        }
        return sum;
    }

    private double norm(Map<Long, Double> weights) {
        double sum = 0.0;
        for (double w : weights.values()) {
            sum += w;
        }
        return Math.sqrt(sum);
    }

    private double ratingOf(UserActionAvro userAction) {
        String type = String.valueOf(userAction.getActionType());
        return switch (type) {
            case "ACTION_LIKE" -> 1.0;
            case "ACTION_REGISTER" -> 0.8;
            case "ACTION_VIEW" -> 0.4;
            default -> 0.0;
        };
    }

    public void put(long eventA, long eventB, double similarity) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        similarityMatrix
                .computeIfAbsent(first, e -> new ConcurrentHashMap<>())
                .put(second, similarity);
    }

    public double get(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return similarityMatrix
                .computeIfAbsent(first, e -> new ConcurrentHashMap<>())
                .getOrDefault(second, 0.0);
    }
}


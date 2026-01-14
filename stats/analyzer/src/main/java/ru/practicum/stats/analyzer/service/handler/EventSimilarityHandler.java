package ru.practicum.stats.analyzer.service.handler;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stats.recommendations.RecommendedEventProto;
import ru.practicum.stats.analyzer.dal.model.EventSimilarity;
import ru.practicum.stats.analyzer.dal.model.UserAction;
import ru.practicum.stats.analyzer.dal.repo.InteractionsRepository;
import ru.practicum.stats.analyzer.dal.repo.SimilaritiesRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class EventSimilarityHandler {
    private static final int K = 5;

    private final InteractionsRepository interactionsRepository;
    private final SimilaritiesRepository similaritiesRepository;

    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, long maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        List<UserAction> userEventList = interactionsRepository.findByUserId(userId);
        if (userEventList.isEmpty()) {
            return List.of();
        }

        Set<Long> userEventsFull = userEventList.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        Map<Long, Double> userRatingByEvent = new HashMap<>(userEventsFull.size());
        for (UserAction ua : userEventList) {
            userRatingByEvent.merge(ua.getEventId(), ua.getRating(), Math::max);
        }

        Set<Long> userEvents = userEventList.stream()
                .sorted(Comparator.comparing(UserAction::getTs).reversed())
                .map(UserAction::getEventId)
                .limit(maxResults)
                .collect(Collectors.toSet());

        if (userEvents.isEmpty()) {
            return List.of();
        }

        Set<Long> notUserEvents = interactionsRepository.findEventIdsNotInteractedByUser(userId);
        if (notUserEvents.isEmpty()) {
            return List.of();
        }

        List<EventSimilarity> crossSimilarities = similaritiesRepository.findBetweenEventSets(userEvents, notUserEvents);
        if (crossSimilarities.isEmpty()) {
            return List.of();
        }

        crossSimilarities.sort(Comparator.comparing(EventSimilarity::getSimilarity).reversed());
        if (crossSimilarities.size() > maxResults) {
            crossSimilarities = crossSimilarities.subList(0, (int) maxResults);
        }

        Map<Long, Double> sortedEvents = new HashMap<>();
        for (EventSimilarity sim : crossSimilarities) {
            Long candidate = notUserEvents.contains(sim.getEventA()) ? sim.getEventA() : sim.getEventB();
            if (!sortedEvents.containsKey(candidate)) {
                sortedEvents.put(candidate, sim.getSimilarity());
            }
            if (sortedEvents.size() == maxResults) {
                break;
            }
        }

        if (sortedEvents.isEmpty()) {
            return List.of();
        }

        List<EventSimilarity> candidateToUser = similaritiesRepository.findBetweenEventSets(sortedEvents.keySet(), userEventsFull);

        Map<Long, List<EventSimilarity>> simsByCandidate = new HashMap<>(sortedEvents.size());
        for (EventSimilarity sim : candidateToUser) {
            Long candidate = sortedEvents.containsKey(sim.getEventA()) ? sim.getEventA()
                    : (sortedEvents.containsKey(sim.getEventB()) ? sim.getEventB() : null);
            if (candidate != null) {
                simsByCandidate.computeIfAbsent(candidate, k -> new ArrayList<>()).add(sim);
            }
        }

        List<RecommendedEventProto> results = new ArrayList<>(sortedEvents.size());
        for (Long eventId : sortedEvents.keySet()) {
            List<EventSimilarity> sims = simsByCandidate.getOrDefault(eventId, List.of());

            if (!sims.isEmpty()) {
                sims.sort(Comparator.comparing(EventSimilarity::getSimilarity).reversed());
                if (sims.size() > K) {
                    sims = sims.subList(0, K);
                }
            }

            double sumWeightedEstimates = 0.0;
            double coefSum = 0.0;

            for (EventSimilarity sim : sims) {
                coefSum += sim.getSimilarity();

                Long neighbor = Objects.equals(sim.getEventA(), eventId) ? sim.getEventB() : sim.getEventA();
                Double rating = userRatingByEvent.get(neighbor);
                if (rating != null) {
                    sumWeightedEstimates += rating * sim.getSimilarity();
                }
            }

            double score = sumWeightedEstimates / coefSum;
            double scoreFormat = BigDecimal.valueOf(score)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            results.add(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(scoreFormat)
                    .build());
        }

        return results;
    }

    public List<RecommendedEventProto> getSimilarEvents(Long userId, Long eventId, long maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        List<EventSimilarity> eventSimilarityList = similaritiesRepository.findByEventAOrEventB(eventId);
        if (eventSimilarityList.isEmpty()) {
            return List.of();
        }

        Set<Long> userItemIds = interactionsRepository.findByUserId(userId).stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        return eventSimilarityList.stream()
                .filter(sim -> !userItemIds.contains(sim.getEventA()) || !userItemIds.contains(sim.getEventB()))
                .map(sim -> mapperToProto(
                        Objects.equals(sim.getEventA(), eventId) ? sim.getEventB() : sim.getEventA(),
                        sim.getSimilarity()
                ))
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    public List<RecommendedEventProto> getInteractionsCount(Set<Long> eventsIds) {
        if (eventsIds == null || eventsIds.isEmpty()) {
            return List.of();
        }

        List<InteractionsRepository.EventScoreSum> sums = interactionsRepository.sumRatingsByEventIds(eventsIds);
        Map<Long, Double> scoreByEvent = new HashMap<>(sums.size());
        for (InteractionsRepository.EventScoreSum row : sums) {
            scoreByEvent.put(row.getEventId(), row.getScore());
        }

        List<RecommendedEventProto> recEventProtoList = new ArrayList<>(eventsIds.size());
        for (Long eventId : eventsIds) {
            recEventProtoList.add(mapperToProto(eventId, scoreByEvent.getOrDefault(eventId, 0.0)));
        }

        return recEventProtoList.stream()
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .collect(Collectors.toList());
    }

    private RecommendedEventProto mapperToProto(Long eventId, Double sum) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(sum)
                .build();
    }
}

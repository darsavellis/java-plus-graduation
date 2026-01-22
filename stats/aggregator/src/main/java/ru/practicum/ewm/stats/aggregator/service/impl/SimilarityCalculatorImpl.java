package ru.practicum.ewm.stats.aggregator.service.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.service.SimilarityCalculator;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimilarityCalculatorImpl implements SimilarityCalculator {
    final static double VIEW_WEIGHT = 0.4;
    final static double REGISTER_WEIGHT = 0.8;
    final static double LIKE_WEIGHT = 1.0;

    final Map<Long, Map<Long, Double>> weightMatrix = new ConcurrentHashMap<>();
    final Map<Long, Double> weightEventSums = new ConcurrentHashMap<>();
    final Map<Long, Map<Long, Double>> minWeightSum = new ConcurrentHashMap<>();
    final Map<Long, Set<Long>> eventsByUser = new ConcurrentHashMap<>();

    @Override
    public List<EventSimilarityAvro> calculateSimilarity(UserActionAvro userActionAvro) {
        long userId = userActionAvro.getUserId();
        long eventId = userActionAvro.getEventId();
        double actionWeight = getActionWeight(userActionAvro.getActionType());

        log.debug("Calculating similarity for userId={}, eventId={}, actionType={}", userId, eventId, userActionAvro.getActionType());

        Map<Long, Double> eventWeights = weightMatrix.computeIfAbsent(eventId, k -> new HashMap<>());
        double oldWeight = eventWeights.getOrDefault(userId, 0.0);

        if (actionWeight > oldWeight) {
            eventWeights.put(userId, actionWeight);
            weightMatrix.put(eventId, eventWeights);

            double totalWeight = weightEventSums.getOrDefault(eventId, 0.0);
            totalWeight = totalWeight - oldWeight + actionWeight;
            weightEventSums.put(eventId, totalWeight);

            Set<Long> userEvents = eventsByUser.computeIfAbsent(userId, k -> new HashSet<>());

            List<EventSimilarityAvro> similarities = new ArrayList<>();

            for (Long otherEventId : userEvents) {
                if (!otherEventId.equals(eventId)) {
                    double similarity = recalculateSimilarity(eventId, otherEventId, userId, oldWeight, actionWeight);
                    similarities.add(toAvro(eventId, otherEventId, similarity));
                    log.trace("Calculated similarity between eventId={} and otherEventId={} for userId={}: {}", eventId, otherEventId, userId, similarity);
                }
            }

            userEvents.add(eventId);
            log.info("Updated weights and calculated similarities for userId={}, eventId={}", userId, eventId);
            return similarities;
        }
        log.debug("No similarity calculation needed for userId={}, eventId={} (actionWeight <= oldWeight)", userId, eventId);
        return Collections.emptyList();
    }

    double getActionWeight(ActionTypeAvro actionTypeAvro) {
        double weight = switch (actionTypeAvro) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
        log.trace("ActionType {} mapped to weight {}", actionTypeAvro, weight);
        return weight;
    }

    double recalculateSimilarity(long eventA, long eventB, long userId, double oldWeight, double newWeight) {
        double similarEventWeight = weightMatrix.getOrDefault(eventB, Map.of()).getOrDefault(userId, 0.0);

        double minOld = Math.min(oldWeight, similarEventWeight);
        double minNew = Math.min(newWeight, similarEventWeight);
        double difference = minNew - minOld;

        double minSum = getMinSum(eventA, eventB) + difference;
        getMinSum(eventA, eventB, minSum);

        double totalSumEventA = weightEventSums.getOrDefault(eventA, 0.0);
        double totalSumEventB = weightEventSums.getOrDefault(eventB, 0.0);

        double result = minSum / (Math.sqrt(totalSumEventA) * Math.sqrt(totalSumEventB));
        log.trace("Recalculated similarity: eventA={}, eventB={}, userId={}, result={}", eventA, eventB, userId, result);
        return result;
    }

    void getMinSum(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightSum.computeIfAbsent(first, k -> new HashMap<>()).put(second, sum);
        log.trace("Updated minSum for eventA={}, eventB={}, sum={}", first, second, sum);
    }

    double getMinSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        double sum = minWeightSum.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
        log.trace("Fetched minSum for eventA={}, eventB={}, sum={}", first, second, sum);
        return sum;
    }

    EventSimilarityAvro toAvro(long eventA, long eventB, double score) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        EventSimilarityAvro avro = EventSimilarityAvro.newBuilder()
            .setEventA(first)
            .setEventB(second)
            .setScore(score)
            .setTimestamp(Instant.now())
            .build();
        log.debug("Created EventSimilarityAvro: {}", avro);
        return avro;
    }
}

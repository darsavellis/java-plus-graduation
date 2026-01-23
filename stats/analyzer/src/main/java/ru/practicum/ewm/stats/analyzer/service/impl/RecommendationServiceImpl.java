package ru.practicum.ewm.stats.analyzer.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.analyzer.dal.model.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.dal.model.UserAction;
import ru.practicum.ewm.stats.analyzer.dal.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.dal.repository.UserActionRepository;
import ru.practicum.ewm.stats.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.grpc.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.UserPredictionsRequestProto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationServiceImpl implements RecommendationService {
    static final int MAX_NEIGHBOURS = 5;

    final UserActionRepository userActionRepository;
    final EventSimilarityRepository eventSimilarityRepository;

    @Override
    public Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        log.info("getRecommendationsForUser: userId={}, maxResults={}", request.getUserId(), request.getMaxResults());
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        PageRequest userEventsPageRequest = createPageRequest(maxResults, "timestamp");
        List<Long> userEventIds = userActionRepository.findByUserId(userId, userEventsPageRequest);
        log.debug("User {} recent event IDs: {}", userId, userEventIds);

        if (userEventIds.isEmpty()) {
            log.info("No events found for user {}. Returning empty recommendations.", userId);
            return Stream.empty();
        }

        PageRequest similaritiesPageRequest = createPageRequest(maxResults, "score");
        List<EventSimilarity> similarities = eventSimilarityRepository.findNewSimilar(userEventIds, similaritiesPageRequest);
        log.debug("Found {} new similarities for user {}.", similarities.size(), userId);

        Map<Long, Double> predictedScores = predict(similarities, new HashSet<>(userEventIds));
        log.info("Predicted scores for user {}: {}", userId, predictedScores);

        return mapToProto(predictedScores);
    }

    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        log.info("getSimilarEvents: eventId={}, userId={}, maxResults={}", request.getEventId(), request.getUserId(), request.getMaxResults());
        long eventId = request.getEventId();
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        List<EventSimilarity> similarities = eventSimilarityRepository.findAllByEventId(eventId);
        log.debug("Found {} similarities for event {}.", similarities.size(), eventId);
        Set<Long> userEventIds = userActionRepository.findByUserIdExcludeEventId(userId, eventId);
        log.debug("User {} has interacted with events (excluding {}): {}", userId, eventId, userEventIds);

        List<EventSimilarity> filteredSimilarities = filterSimilaritiesByUserEvents(similarities, userEventIds);
        log.debug("Filtered similarities count: {}", filteredSimilarities.size());
        List<EventSimilarity> topSimilarities = getTopNSimilarities(filteredSimilarities, maxResults);
        log.info("Returning top {} similar events for event {} and user {}.", topSimilarities.size(), eventId, userId);

        return mapToProto(topSimilarities, eventId);
    }

    @Override
    public Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        log.info("getInteractionsCount: eventIds={}", request.getEventIdList());
        List<UserAction> userActions = userActionRepository.findByEventIdIn(new HashSet<>(request.getEventIdList()));
        log.debug("Found {} user actions for events {}.", userActions.size(), request.getEventIdList());

        Map<Long, Double> sumOfWeightsByEvent = userActions.stream()
            .collect(Collectors.groupingBy(
                UserAction::getEventId,
                Collectors.summingDouble(UserAction::getWeight)
            ));
        log.info("Sum of weights by event: {}", sumOfWeightsByEvent);

        return mapToProto(sumOfWeightsByEvent);
    }

    Map<Long, Double> predict(List<EventSimilarity> similarities, Set<Long> userEvents) {
        log.debug("predict: similarities.size={}, userEvents.size={}", similarities.size(), userEvents.size());
        Map<Long, Double> scoreByEvent = new HashMap<>();

        for (EventSimilarity similarity : similarities) {
            long candidateEventId = getOtherEventId(similarity, userEvents);
            log.trace("Processing candidate eventId: {}", candidateEventId);

            PageRequest pageRequest = createPageRequest(MAX_NEIGHBOURS, "score");
            List<EventSimilarity> neighbours = eventSimilarityRepository.findNeighbours(candidateEventId, pageRequest);
            log.trace("Found {} neighbours for candidate event {}.", neighbours.size(), candidateEventId);

            List<Long> neighbourIds = neighbours.stream()
                .map(n -> getOtherEventId(n, candidateEventId))
                .toList();
            log.trace("Neighbour IDs for candidate event {}: {}", candidateEventId, neighbourIds);

            List<UserAction> userActions = userActionRepository.findByEventIdIn(new HashSet<>(neighbourIds));
            log.trace("User actions for neighbours: {}", userActions);

            double predictedScore = calculatePredictedScore(neighbours, userActions, candidateEventId);
            log.debug("Predicted score for event {}: {}", candidateEventId, predictedScore);

            scoreByEvent.put(candidateEventId, predictedScore);
        }

        log.debug("Final predicted scores: {}", scoreByEvent);
        return scoreByEvent;
    }

    double calculatePredictedScore(List<EventSimilarity> neighbours, List<UserAction> userActions, long candidateId) {
        log.trace("calculatePredictedScore: candidateId={}, neighbours.size={}, userActions.size={}", candidateId, neighbours.size(), userActions.size());
        Map<Long, Double> weightByEvent = userActions.stream()
            .collect(Collectors.toMap(UserAction::getEventId, UserAction::getWeight));

        double sumOfWeightedScores = 0.0;
        double sumOfSimilarities = 0.0;

        for (EventSimilarity neighbour : neighbours) {
            long neighbourId = getOtherEventId(neighbour, candidateId);
            double weight = weightByEvent.getOrDefault(neighbourId, 0.0);
            double similarity = neighbour.getScore();

            sumOfWeightedScores += weight * similarity;
            sumOfSimilarities += similarity;
            log.trace("NeighbourId={}, weight={}, similarity={}, sumOfWeightedScores={}, sumOfSimilarities={}", neighbourId, weight, similarity, sumOfWeightedScores, sumOfSimilarities);
        }

        double result = sumOfSimilarities == 0 ? 0.0 : sumOfWeightedScores / sumOfSimilarities;
        log.debug("Predicted score for candidateId {}: {}", candidateId, result);
        return result;
    }

    Stream<RecommendedEventProto> mapToProto(List<EventSimilarity> eventSimilarities, long currentEventId) {
        return eventSimilarities.stream()
            .map(s -> {
                long recommended = getOtherEventId(s, currentEventId);
                return RecommendedEventProto.newBuilder()
                    .setEventId(recommended)
                    .setScore(s.getScore())
                    .build();
            });
    }

    Stream<RecommendedEventProto> mapToProto(Map<Long, Double> scoreByEvent) {
        return scoreByEvent.entrySet().stream()
            .map(entry -> RecommendedEventProto.newBuilder()
                .setEventId(entry.getKey())
                .setScore(entry.getValue())
                .build());
    }

    PageRequest createPageRequest(int size, String sortBy) {
        return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, sortBy));
    }

    List<EventSimilarity> filterSimilaritiesByUserEvents(List<EventSimilarity> similarities, Set<Long> userEventIds) {
        return similarities.stream()
            .filter(s -> !userEventIds.contains(s.getEventA()) && !userEventIds.contains(s.getEventB()))
            .collect(Collectors.toList());
    }

    List<EventSimilarity> getTopNSimilarities(List<EventSimilarity> similarities, int n) {
        return similarities.stream()
            .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
            .limit(n)
            .collect(Collectors.toList());
    }

    long getOtherEventId(EventSimilarity similarity, long eventId) {
        return similarity.getEventA() == eventId ? similarity.getEventB() : similarity.getEventA();
    }

    long getOtherEventId(EventSimilarity similarity, Set<Long> eventIds) {
        return eventIds.contains(similarity.getEventA()) ? similarity.getEventB() : similarity.getEventA();
    }
}

package ru.practicum.ewm.stats.analyzer.service;

import ru.practicum.ewm.stats.grpc.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.UserPredictionsRequestProto;

import java.util.stream.Stream;

public interface RecommendationService {
    Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request);
    Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request);
    Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request);
}

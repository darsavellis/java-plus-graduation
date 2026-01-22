package ru.practicum.ewm.stats.analyzer.dal.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface SimilarityHandler {
    void handle(EventSimilarityAvro eventSimilarityAvro);
}

package ru.practicum.ewm.stats.analyzer.dal.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.dal.model.EventSimilarity;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
public class EventSimilarityMapper {
    public EventSimilarity mapToEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        return EventSimilarity.builder()
            .eventA(eventSimilarityAvro.getEventA())
            .eventB(eventSimilarityAvro.getEventB())
            .score(eventSimilarityAvro.getScore())
            .timestamp(eventSimilarityAvro.getTimestamp())
            .build();
    }
}

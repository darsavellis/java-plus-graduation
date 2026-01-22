package ru.practicum.ewm.stats.analyzer.dal.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.analyzer.dal.mapper.EventSimilarityMapper;
import ru.practicum.ewm.stats.analyzer.dal.model.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.dal.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.dal.service.SimilarityHandler;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimilarityHandlerImpl implements SimilarityHandler {
    final EventSimilarityMapper similarityMapper;
    final EventSimilarityRepository similarityRepository;

    @Override
    public void handle(EventSimilarityAvro similarityAvro) {
        log.debug("Handling EventSimilarityAvro: {}", similarityAvro);
        try {
            Optional<EventSimilarity> similarity = similarityRepository.findByEventAAndEventB(
                similarityAvro.getEventA(), similarityAvro.getEventB());

            similarity.map(existing -> {
                if (existing.getScore() != similarityAvro.getScore()) {
                    log.info("Updating score for eventA={}, eventB={} from {} to {}", existing.getEventA(), existing.getEventB(), existing.getScore(), similarityAvro.getScore());
                    existing.setScore(similarityAvro.getScore());
                } else {
                    log.debug("No update needed for eventA={}, eventB={} (score unchanged)", existing.getEventA(), existing.getEventB());
                }
                return similarityRepository.save(existing);
            }).orElseGet(() -> {
                log.info("Saving new EventSimilarity for eventA={}, eventB={}", similarityAvro.getEventA(), similarityAvro.getEventB());
                return similarityRepository.save(similarityMapper.mapToEventSimilarity(similarityAvro));
            });
        } catch (Exception e) {
            log.error("Error handling EventSimilarityAvro: {}", similarityAvro, e);
            throw e;
        }
    }
}

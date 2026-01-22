package ru.practicum.ewm.stats.aggregator.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.config.KafkaClientConfig;
import ru.practicum.ewm.stats.aggregator.config.KafkaProducerClient;
import ru.practicum.ewm.stats.aggregator.service.SimilarityCalculator;
import ru.practicum.ewm.stats.aggregator.service.UserActionHandler;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionHandlerImpl implements UserActionHandler {
    final SimilarityCalculator calculator;
    final KafkaProducerClient kafkaProducerClient;
    final KafkaClientConfig kafkaClientConfig;

    public void handle(ConsumerRecord<Long, SpecificRecord> record) {
        log.debug("Handling ConsumerRecord: key={}, value={}, offset={}, partition={}", record.key(), record.value(), record.offset(), record.partition());
        UserActionAvro userActionAvro = (UserActionAvro) record.value();
        List<EventSimilarityAvro> eventSimilarityAvros = calculator.calculateSimilarity(userActionAvro);
        log.info("Calculated {} similarities for userId={}, eventId={}", eventSimilarityAvros.size(), userActionAvro.getUserId(), userActionAvro.getEventId());

        String topicName = kafkaClientConfig.getProducerConfig().getTopics().get("events-similarity");
        log.trace("Producer topic resolved: {}", topicName);

        for (EventSimilarityAvro similarityAvro : eventSimilarityAvros) {
            String key = similarityAvro.getEventA() + "-" + similarityAvro.getEventB();
            try {
                kafkaProducerClient.getProducer().send(new ProducerRecord<>(
                    topicName,
                    null,
                    similarityAvro.getTimestamp().toEpochMilli(),
                    key,
                    similarityAvro));
                log.debug("Sent EventSimilarityAvro to topic '{}', key={}, value={}", topicName, key, similarityAvro);
            } catch (Exception e) {
                log.error("Failed to send EventSimilarityAvro to Kafka: key={}, value={}", key, similarityAvro, e);
            }
        }
    }
}

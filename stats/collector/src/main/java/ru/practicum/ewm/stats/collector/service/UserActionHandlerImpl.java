package ru.practicum.ewm.stats.collector.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.config.KafkaClient;
import ru.practicum.ewm.stats.collector.config.KafkaProducerConfig;
import ru.practicum.ewm.stats.collector.mapper.UserActionMapper;
import ru.practicum.ewm.stats.grpc.UserActionProto;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionHandlerImpl implements UserActionHandler {
    final KafkaClient kafkaClient;
    final KafkaProducerConfig kafkaProducerConfig;
    final UserActionMapper userActionMapper;

    @Override
    public void handle(UserActionProto request) {
        log.debug("Received UserActionProto: {}", request);
        UserActionAvro userActionAvro = userActionMapper.mapToUserActionAvro(request);
        log.info("Mapped UserActionAvro: {}", userActionAvro);
        String topicName = kafkaProducerConfig.getTopics().get("user-actions");
        log.trace("Kafka topic resolved: {}", topicName);
        try {
            kafkaClient.getProducer().send(new ProducerRecord<>(
                topicName,
                null,
                userActionAvro.getTimestamp().toEpochMilli(),
                userActionAvro.getUserId(),
                userActionAvro));
            log.info("User action sent to Kafka topic '{}', userId={}, timestamp={}", topicName, userActionAvro.getUserId(), userActionAvro.getTimestamp());
        } catch (Exception exception) {
            log.error("Failed to send user action to Kafka", exception);
            throw exception;
        }
    }
}

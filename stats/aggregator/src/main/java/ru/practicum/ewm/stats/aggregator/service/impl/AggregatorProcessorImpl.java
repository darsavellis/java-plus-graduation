package ru.practicum.ewm.stats.aggregator.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.config.KafkaClientConfig;
import ru.practicum.ewm.stats.aggregator.config.KafkaConsumerClient;
import ru.practicum.ewm.stats.aggregator.service.AggregatorProcessor;
import ru.practicum.ewm.stats.aggregator.service.UserActionHandler;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregatorProcessorImpl implements AggregatorProcessor {
    static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    final KafkaConsumerClient consumerClient;
    final UserActionHandler userActionHandler;
    final KafkaClientConfig config;

    public void start() {
        KafkaConsumer<Long, SpecificRecord> consumer = consumerClient.getConsumer();
        String userActionsTopic = config.getConsumerConfig().getTopics().get("user-actions");

        log.debug("KafkaConsumer instance created: {}", consumer);
        log.trace("User actions topic resolved: {}", userActionsTopic);

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(List.of(userActionsTopic));
            log.info("Subscribed to topic: {}", userActionsTopic);

            while (true) {
                ConsumerRecords<Long, SpecificRecord> consumerRecords = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                log.debug("Polled {} records from topic {}", consumerRecords.count(), userActionsTopic);
                processRecord(consumerRecords);
            }
        } catch (WakeupException exception) {
            log.info("Consumer wakeup");
        } catch (Exception exception) {
            log.error("Error while processing messages", exception);
        } finally {
            try {
                consumerClient.stopConsumer();
                log.info("Consumer stopped");
            } catch (Exception exception) {
                log.error("Error stopping consumer", exception);
            }
        }
    }

    void processRecord(ConsumerRecords<Long, SpecificRecord> consumerRecords) {
        for (ConsumerRecord<Long, SpecificRecord> record : consumerRecords) {
            log.trace("Processing record: key={}, value={}, offset={}, partition={}", record.key(), record.value(), record.offset(), record.partition());
            try {
                userActionHandler.handle(record);
                log.debug("Record handled successfully: offset={}, partition={}", record.offset(), record.partition());
            } catch (Exception e) {
                log.error("Error handling record: offset={}, partition={}", record.offset(), record.partition(), e);
            }
        }
    }
}

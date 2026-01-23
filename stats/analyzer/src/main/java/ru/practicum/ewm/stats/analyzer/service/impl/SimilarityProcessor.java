package ru.practicum.ewm.stats.analyzer.service.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.config.KafkaConfig;
import ru.practicum.ewm.stats.analyzer.service.SimilarityHandler;
import ru.practicum.ewm.stats.analyzer.service.Processor;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimilarityProcessor implements Processor<EventSimilarityAvro>, Runnable {
    final KafkaConsumer<Long, EventSimilarityAvro> consumer;
    final KafkaConfig.ConsumerConfig consumerConfig;
    final SimilarityHandler similarityHandler;

    public SimilarityProcessor(KafkaConfig kafkaConfig, SimilarityHandler similarityHandler) {
        this.consumerConfig = kafkaConfig.getConsumers().get(this.getClass().getSimpleName());
        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.similarityHandler = similarityHandler;
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
    }

    public void run() {
        start();
    }

    @Override
    public void start() {
        log.info("Starting SimilarityProcessor. Subscribing to topics: {}", consumerConfig.getTopics());
        consumer.subscribe(consumerConfig.getTopics());
        try {
            while (true) {
                ConsumerRecords<Long, EventSimilarityAvro> consumerRecords = consumer.poll(consumerConfig.getPollTimeout());
                log.debug("Polled {} records from topics {}", consumerRecords.count(), consumerConfig.getTopics());
                if (!consumerRecords.isEmpty()) {
                    for (ConsumerRecord<Long, EventSimilarityAvro> consumerRecord : consumerRecords) {
                        log.trace("Processing record: key={}, value={}, offset={}, partition={}", consumerRecord.key(), consumerRecord.value(), consumerRecord.offset(), consumerRecord.partition());
                        processRecord(consumerRecord);
                    }
                    consumer.commitSync();
                    log.info("Committed offsets after processing batch");
                }
            }
        } catch (WakeupException exception) {
            log.info("Consumer wakeup signal received, shutting down");
        } catch (Exception exception) {
            log.error("Error while processing messages in SimilarityProcessor", exception);
        } finally {
            stop();
            log.info("SimilarityProcessor stopped");
        }
    }

    @Override
    public void stop() {
        try {
            consumer.close();
            log.info("Kafka consumer closed successfully");
        } catch (Exception e) {
            log.error("Error closing Kafka consumer", e);
        }
    }

    @Override
    public void processRecord(ConsumerRecord<Long, EventSimilarityAvro> consumerRecord) {
        EventSimilarityAvro eventSimilarityAvro = consumerRecord.value();
        log.debug("Handling EventSimilarityAvro: {}", eventSimilarityAvro);
        try {
            similarityHandler.handle(eventSimilarityAvro);
            log.info("Successfully handled EventSimilarityAvro: {}", eventSimilarityAvro);
        } catch (Exception e) {
            log.error("Error handling EventSimilarityAvro: {}", eventSimilarityAvro, e);
        }
    }
}

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
import ru.practicum.ewm.stats.analyzer.dal.service.UserActionHandler;
import ru.practicum.ewm.stats.analyzer.service.Processor;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionProcessor implements Processor<UserActionAvro>, Runnable {
    final KafkaConsumer<Long, UserActionAvro> consumer;
    final KafkaConfig.ConsumerConfig consumerConfig;

    private final UserActionHandler userActionHandler;

    public UserActionProcessor(KafkaConfig kafkaConfig, UserActionHandler userActionHandler) {
        this.consumerConfig = kafkaConfig.getConsumers().get(this.getClass().getSimpleName());
        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.userActionHandler = userActionHandler;
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
    }

    @Override
    public void run() {
        start();
    }

    @Override
    public void start() {
        log.info("Starting UserActionProcessor. Subscribing to topics: {}", consumerConfig.getTopics());
        consumer.subscribe(consumerConfig.getTopics());
        try {
            while (true) {
                ConsumerRecords<Long, UserActionAvro> consumerRecords = consumer.poll(consumerConfig.getPollTimeout());
                log.debug("Polled {} records from topics {}", consumerRecords.count(), consumerConfig.getTopics());
                if (!consumerRecords.isEmpty()) {
                    for (ConsumerRecord<Long, UserActionAvro> consumerRecord : consumerRecords) {
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
            log.error("Error while processing messages in UserActionProcessor", exception);
        } finally {
            stop();
            log.info("UserActionProcessor stopped");
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
    public void processRecord(ConsumerRecord<Long, UserActionAvro> consumerRecord) {
        UserActionAvro userActionAvro = consumerRecord.value();
        log.debug("Handling UserActionAvro: {}", userActionAvro);
        try {
            userActionHandler.handle(userActionAvro);
            log.info("Successfully handled UserActionAvro: {}", userActionAvro);
        } catch (Exception e) {
            log.error("Error handling UserActionAvro: {}", userActionAvro, e);
        }
    }
}

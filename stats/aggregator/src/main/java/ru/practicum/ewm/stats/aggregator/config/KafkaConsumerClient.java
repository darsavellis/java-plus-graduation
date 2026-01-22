package ru.practicum.ewm.stats.aggregator.config;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public interface KafkaConsumerClient {
    KafkaConsumer<Long, SpecificRecord> getConsumer();

    void stopConsumer();
}

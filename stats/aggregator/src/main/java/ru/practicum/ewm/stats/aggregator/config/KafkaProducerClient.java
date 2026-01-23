package ru.practicum.ewm.stats.aggregator.config;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;

public interface KafkaProducerClient {
    KafkaProducer<String, SpecificRecord> getProducer();

    void stopProducer();
}

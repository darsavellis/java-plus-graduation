package ru.practicum.ewm.stats.analyzer.service;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface Processor<T extends SpecificRecord> {
    void start();

    void stop();

    void processRecord(ConsumerRecord<Long, T> consumerRecord);
}

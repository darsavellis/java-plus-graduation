package ru.practicum.ewm.stats.aggregator.service;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface UserActionHandler {
    void handle(ConsumerRecord<Long, SpecificRecord> record);
}

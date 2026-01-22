package ru.practicum.ewm.stats.collector.config;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
@RequiredArgsConstructor
public class KafkaClientConfig {
    final KafkaProducerConfig config;

    @Bean
    KafkaClient getClient() {
        return new KafkaClient() {
            KafkaProducer<Long, SpecificRecordBase> producer;

            @Override
            public Producer<Long, SpecificRecordBase> getProducer() {
                if (Objects.isNull(producer)) {
                    initProducer();
                }
                return producer;
            }

            private void initProducer() {
                producer = new KafkaProducer<>(config.getProperties());
            }

            @Override
            public void stop() {
                if (Objects.nonNull(producer)) {
                    producer.flush();
                    producer.close();
                }
            }
        };
    }
}

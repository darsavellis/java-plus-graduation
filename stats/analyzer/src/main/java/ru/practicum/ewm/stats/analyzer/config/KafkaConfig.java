package ru.practicum.ewm.stats.analyzer.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@ConfigurationProperties("analyzer.kafka")
public class KafkaConfig {
    Properties commonProperties;
    Map<String, ConsumerConfig> consumers;

    public KafkaConfig(Properties commonProperties, List<ConsumerConfig> consumers) {
        this.consumers = consumers.stream().peek(consumerConfig -> {
            consumerConfig.getProperties().putAll(commonProperties);
        }).collect(Collectors.toMap(ConsumerConfig::getType, Function.identity()));
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ConsumerConfig {
        final String type;
        final Properties properties = new Properties();
        final List<String> topics;
        final Duration pollTimeout;
        final Integer commitInterval;

        public ConsumerConfig(String type, Properties properties, List<String> topics, Duration pollTimeout, Integer commitInterval) {
            this.type = type;
            this.properties.putAll(properties);
            this.topics = topics;
            this.pollTimeout = pollTimeout;
            this.commitInterval = commitInterval;
        }
    }
}

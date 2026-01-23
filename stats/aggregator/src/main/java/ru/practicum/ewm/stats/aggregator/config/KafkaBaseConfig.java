package ru.practicum.ewm.stats.aggregator.config;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaBaseConfig {
    Properties properties;
    Map<String, String> topics;
}

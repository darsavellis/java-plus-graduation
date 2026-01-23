package ru.practicum.ewm.stats.aggregator;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.service.AggregatorProcessor;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregatorRunner implements CommandLineRunner {
    final AggregatorProcessor aggregatorProcessor;

    @Override
    public void run(String... args) {
        aggregatorProcessor.start();
    }
}

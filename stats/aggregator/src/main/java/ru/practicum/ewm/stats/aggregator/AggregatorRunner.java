package ru.practicum.ewm.stats.aggregator;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.service.impl.AggregatorProcessorImpl;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregatorRunner implements CommandLineRunner {
    final AggregatorProcessorImpl aggregatorProcessor;

    @Override
    public void run(String... args) {
        aggregatorProcessor.start();
    }
}

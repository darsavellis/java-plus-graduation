package ru.practicum.ewm.stats.analyzer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.service.impl.SimilarityProcessor;
import ru.practicum.ewm.stats.analyzer.service.impl.UserActionProcessor;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnalyzerRunner implements CommandLineRunner {
    final SimilarityProcessor similarityProcessor;
    final UserActionProcessor userActionProcessor;

    @Override
    public void run(String... args) throws Exception {
        Thread userActionThread = new Thread(userActionProcessor);
        userActionThread.start();
        similarityProcessor.start();
    }
}

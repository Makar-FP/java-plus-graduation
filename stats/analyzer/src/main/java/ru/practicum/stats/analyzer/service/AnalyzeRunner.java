package ru.practicum.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyzeRunner implements CommandLineRunner {
    final UserActionProcessor userActionProcessor;
    final EventSimilarityProcessor eventSimilarityProcessor;

    @Override
    public void run(String... args) {
        Thread userActionThread = new Thread(userActionProcessor, "UserActionHandlerThread");
        userActionThread.setDaemon(false);
        userActionThread.start();

        Thread similarityThread = new Thread(eventSimilarityProcessor, "EventSimilarityHandlerThread");
        similarityThread.setDaemon(false);
        similarityThread.start();
    }
}
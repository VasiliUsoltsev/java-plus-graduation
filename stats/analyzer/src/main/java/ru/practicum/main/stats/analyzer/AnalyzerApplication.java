package ru.practicum.main.stats.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyzerApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApplication.class, args);

        AnalyzerStarterUserAction starterUserAction = context.getBean(AnalyzerStarterUserAction.class);
        Thread threadUserAction = new Thread(starterUserAction::start, "Analyzer-consumer-user-action");
        threadUserAction.setDaemon(false);
        threadUserAction.start();

        AnalyzerStarterEventSimilarity starterEventSimilarity = context.getBean(AnalyzerStarterEventSimilarity.class);
        Thread threadEventSimilarity = new Thread(starterEventSimilarity::start, "Analyzer-consumer-event-similarity");
        threadEventSimilarity.setDaemon(false);
        threadEventSimilarity.start();
    }
}

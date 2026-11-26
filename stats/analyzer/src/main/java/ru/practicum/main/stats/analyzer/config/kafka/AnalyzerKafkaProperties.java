package ru.practicum.main.stats.analyzer.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka")
public class AnalyzerKafkaProperties {
    // base
    private String bootstrapServers;

    // user-actions
    private String topicUserAction;
    private String groupUserAction;
    private String keyDeserializerUserAction;
    private String valueDeserializerUserAction;

    // events-similarity
    private String topicEventSimilarity;
    private String groupEventSimilarity;
    private String keyDeserializerEventSimilarity;
    private String valueDeserializerEventSimilarity;

    private int maxPoll;
}

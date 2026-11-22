package ru.practicum.main.stats.aggregator.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
@Getter
@Setter
public class AggregatorKafkaProperties {
    private String group;
    private String bootstrapServers;
    private String keySerializer;
    private String valueSerializer;
    private String keyDeserializer;
    private String valueDeserializer;
    private String statsUserTopic;
    private String similarityTopic;
    private Integer maxPoll;
}
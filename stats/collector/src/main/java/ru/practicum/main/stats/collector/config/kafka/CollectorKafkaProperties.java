package ru.practicum.main.stats.collector.config.kafka;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
@Getter
@Setter
public class CollectorKafkaProperties {
    private String bootstrapServers;
    private String keySerializer;
    private String valueSerializer;
    private String statsUserTopic;
}

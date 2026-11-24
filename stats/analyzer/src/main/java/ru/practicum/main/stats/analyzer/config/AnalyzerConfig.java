package ru.practicum.main.stats.analyzer.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.main.stats.analyzer.config.kafka.AnalyzerKafkaProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AnalyzerConfig {
    private final AnalyzerKafkaProperties properties;

    @Bean(destroyMethod = "close")
    public KafkaConsumer<Long, UserActionAvro> kafkaConsumerUserAction() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getGroupUserAction());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, properties.getKeyDeserializerUserAction());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, properties.getValueDeserializerUserAction());
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, properties.getMaxPoll());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new KafkaConsumer<>(config);
    }

    @Bean(destroyMethod = "close")
    public KafkaConsumer<Long, EventSimilarityAvro> kafkaConsumerEventSimilarity() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getGroupEventSimilarity());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, properties.getKeyDeserializerEventSimilarity());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, properties.getValueDeserializerEventSimilarity());
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, properties.getMaxPoll());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new KafkaConsumer<>(config);
    }
}

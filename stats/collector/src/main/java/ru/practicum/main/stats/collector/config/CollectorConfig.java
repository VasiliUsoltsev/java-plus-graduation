package ru.practicum.main.stats.collector.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.main.stats.collector.config.kafka.CollectorKafkaProperties;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CollectorConfig {

    private final CollectorKafkaProperties properties;

    @Bean
    public KafkaProducer<String, SpecificRecordBase> producerFactory() {
        log.info(">>> Kafka producer config: bootstrapServers={}, keySerializer={}, valueSerializer={}",
                properties.getBootstrapServers(),
                properties.getKeySerializer(),
                properties.getValueSerializer());
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, properties.getKeySerializer());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, properties.getValueSerializer());

        return new KafkaProducer<>(config);
    }
}
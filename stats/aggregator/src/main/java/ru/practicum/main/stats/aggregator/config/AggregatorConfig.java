package ru.practicum.main.stats.aggregator.config;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.main.stats.aggregator.config.kafka.AggregatorKafkaProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AggregatorConfig {

    private final AggregatorKafkaProperties properties;

    @Bean(destroyMethod = "close")
    public KafkaProducer<Long, SpecificRecordBase> kafkaProducer() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, properties.getKeySerializer());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, properties.getValueSerializer());
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(config);
    }

    @Bean(destroyMethod = "close")
    public KafkaConsumer<Long, UserActionAvro> kafkaConsumer() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "aggregator-consumer");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getGroup());
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, properties.getKeyDeserializer());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, properties.getValueDeserializer());
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, properties.getMaxPoll());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new KafkaConsumer<>(config);
    }
}
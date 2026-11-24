package ru.practicum.main.stats.analyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.main.stats.analyzer.config.kafka.AnalyzerKafkaProperties;
import ru.practicum.main.stats.analyzer.repository.SimilarityRepository;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerStarterEventSimilarity {

    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<Long, EventSimilarityAvro> consumer;
    private final SimilarityRepository similarityRepository;
    private final AnalyzerKafkaProperties properties;

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        log.info("Старт Analyzer-consumer-event-similarity: читаем {}", properties.getTopicEventSimilarity());

        try {
            consumer.subscribe(List.of(properties.getTopicEventSimilarity()));

            while (true) {
                ConsumerRecords<Long, EventSimilarityAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                int count = 0;
                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    handleRecord(record);
                    manageOffsets(record, count, consumer);
                    count++;
                }
            }
        } catch (WakeupException ignored) {
            log.info("Получен сигнал завершения, останавливаем Analyzer-consumer-event-similarity");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий", e);
        } finally {
            try {
                consumer.commitSync(currentOffsets);
            } finally {
                log.debug("Закрываем consumer");
                consumer.close();
            }
        }
    }

    private void handleRecord(ConsumerRecord<Long, EventSimilarityAvro> record) {
        log.debug("topic={}, partition={}, offset={}, value={}",
                record.topic(), record.partition(), record.offset(), record.value());

        EventSimilarityAvro similarityAvro = record.value();

        similarityRepository.upsertSimilarity(similarityAvro.getEventA(),
                similarityAvro.getEventB(),
                similarityAvro.getScore(),
                similarityAvro.getTimestamp());

        log.info("Пара мероприятий - A={} и B={} зафиксировано в БД",
                similarityAvro.getEventA(),
                similarityAvro.getEventB());
    }

    private void manageOffsets(ConsumerRecord<Long, EventSimilarityAvro> record,
                               int count,
                               KafkaConsumer<Long, EventSimilarityAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        consumer.commitAsync(currentOffsets, (offsets, exception) -> {
            if (exception != null) {
                log.error("Ошибка фиксации оффсетов: {}", offsets, exception);
            }
        });
    }
}

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
import ru.practicum.main.stats.analyzer.service.impl.SimilarityBatchService;

import java.time.Duration;
import java.util.ArrayList;
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
    private final SimilarityBatchService similarityBatchService;
    private final AnalyzerKafkaProperties properties;

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        log.info("Старт Analyzer-consumer-event-similarity: читаем {}", properties.getTopicEventSimilarity());

        try {
            consumer.subscribe(List.of(properties.getTopicEventSimilarity()));

            while (true) {
                ConsumerRecords<Long, EventSimilarityAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                if (records.isEmpty()) {
                    continue;
                }

                List<EventSimilarityAvro> batch = new ArrayList<>(records.count());
                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    batch.add(record.value());
                    currentOffsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );
                }

                similarityBatchService.upsertBatch(batch);

                consumer.commitAsync(new HashMap<>(currentOffsets),
                        (offsets, exception) -> {
                            if (exception != null) {
                                log.error("Ошибка фиксации оффсетов: {}", offsets, exception);
                            }
                        });
            }
        } catch (WakeupException ignored) {
            log.info("Получен сигнал завершения, останавливаем Analyzer-consumer-event-similarity");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий", e);
        } finally {
            try {
                consumer.commitSync(currentOffsets);
            } catch (Exception e) {
                log.warn("Не удалось зафиксировать оффсеты при завершении", e);
            } finally {
                log.debug("Закрываем consumer");
                consumer.close();
            }
        }
    }
}
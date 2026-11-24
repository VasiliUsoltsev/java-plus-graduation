package ru.practicum.main.stats.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.main.stats.aggregator.config.kafka.AggregatorKafkaProperties;
import ru.practicum.main.stats.aggregator.exception.KafkaSendException;
import ru.practicum.main.stats.aggregator.service.SimilarityService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private static final long SEND_TIMEOUT_SECONDS = 10L;

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final KafkaProducer<Long, SpecificRecordBase> producer;
    private final SimilarityService similarityService;
    private final AggregatorKafkaProperties properties;

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        log.info("Старт Aggregator: читаем {}", properties.getStatsUserTopic());

        try {
            consumer.subscribe(List.of(properties.getStatsUserTopic()));

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                int count = 0;
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    handleRecord(record);
                    manageOffsets(record, count, consumer);
                    count++;
                }
            }
        } catch (WakeupException ignored) {
            log.info("Получен сигнал завершения, останавливаем Aggregator");
        } catch (Exception e) {
            log.error("Ошибка во время обработки действий пользователей", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync(currentOffsets);
            } finally {
                log.debug("Закрываем producer");
                producer.close();
                log.debug("Закрываем consumer");
                consumer.close();
            }
        }
    }

    private void handleRecord(ConsumerRecord<Long, UserActionAvro> record) {
        log.debug("topic={}, partition={}, offset={}, value={}",
                record.topic(), record.partition(), record.offset(), record.value());

        Long triggerEventId = record.value().getEventId();

        List<EventSimilarityAvro> similarities = similarityService.processUserAction(record.value());
        for (EventSimilarityAvro similarity : similarities) {
            sendSimilarity(similarity, triggerEventId);
        }
    }

    private void sendSimilarity(EventSimilarityAvro similarity, Long eventId) {
        ProducerRecord<Long, SpecificRecordBase> output = new ProducerRecord<>(
                properties.getSimilarityTopic(),
                null,
                similarity.getTimestamp().toEpochMilli(),
                eventId,
                similarity
        );

        try {
            Future<RecordMetadata> send = producer.send(output);
            RecordMetadata metadata = send.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.debug("Сходство отправлено: A={}, B={}, score={}, partition={}, offset={}",
                    similarity.getEventA(), similarity.getEventB(), similarity.getScore(),
                    metadata.partition(), metadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Прервана отправка сходства A={}, B={}",
                    similarity.getEventA(), similarity.getEventB(), e);
            throw new KafkaSendException("Прервана отправка сходства", e);
        } catch (TimeoutException e) {
            log.error("Таймаут отправки сходства A={}, B={}",
                    similarity.getEventA(), similarity.getEventB(), e);
            throw new KafkaSendException("Kafka не ответила вовремя", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Ошибка отправки сходства A={}, B={}",
                    similarity.getEventA(), similarity.getEventB(), cause);
            throw new KafkaSendException("Не удалось отправить сходство", cause);
        }
    }

    private void manageOffsets(ConsumerRecord<Long, UserActionAvro> record,
                               int count,
                               KafkaConsumer<Long, UserActionAvro> consumer) {
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
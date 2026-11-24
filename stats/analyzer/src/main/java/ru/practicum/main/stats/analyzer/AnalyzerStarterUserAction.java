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
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.main.stats.analyzer.config.kafka.AnalyzerKafkaProperties;
import ru.practicum.main.stats.analyzer.repository.InteractionRepository;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerStarterUserAction {

    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final InteractionRepository interactionRepository;
    private final AnalyzerKafkaProperties properties;

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        log.info("Старт Analyzer-consumer-user-action: читаем {}", properties.getTopicUserAction());

        try {
            consumer.subscribe(List.of(properties.getTopicUserAction()));

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
            log.info("Получен сигнал завершения, останавливаем Analyzer-consumer-user-action");
        } catch (Exception e) {
            log.error("Ошибка во время обработки действий пользователей", e);
        } finally {
            try {
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Закрываем consumer");
                consumer.close();
            }
        }
    }

    private void handleRecord(ConsumerRecord<Long, UserActionAvro> record) {
        log.debug("topic={}, partition={}, offset={}, value={}",
                record.topic(), record.partition(), record.offset(), record.value());

        UserActionAvro actionAvro = record.value();

        interactionRepository.upsertInteraction(actionAvro.getUserId(),
                actionAvro.getEventId(),
                getRating(actionAvro.getActionType()),
                actionAvro.getTimestamp()
        );

        log.info("Действие пользователя с id={} по мероприятию с id={} зафиксировано в БД",
                actionAvro.getUserId(),
                actionAvro.getEventId());
    }

    private double getRating(ActionTypeAvro actionType) {
        return switch (actionType) {
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
            case VIEW -> 0.4;
        };
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

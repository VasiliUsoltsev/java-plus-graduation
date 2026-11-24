package ru.practicum.main.stats.collector.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.grpc.UserActionProto;
import ru.practicum.main.stats.collector.config.kafka.CollectorKafkaProperties;
import ru.practicum.main.stats.collector.mapper.CollectorMapper;
import ru.practicum.main.stats.collector.service.UserActionService;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService, AutoCloseable {
    private static final long SEND_TIMEOUT_SECONDS = 10L;

    private final KafkaProducer<Long, SpecificRecordBase> producer;
    private final CollectorKafkaProperties properties;

    @Override
    public void sendUserAction(UserActionProto action) {
        UserActionAvro avro = CollectorMapper.mapToUserActionAvro(action);

        send(
                properties.getStatsUserTopic(),
                avro.getEventId(),
                avro.getTimestamp().toEpochMilli(),
                avro,
                avro.getActionType().toString()
        );
    }

    private void send(String topic,
                      Long key,
                      long timestamp,
                      SpecificRecordBase value,
                      String eventType) {

        ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                value
        );

        try {
            Future<RecordMetadata> send = producer.send(record);
            producer.flush();
            RecordMetadata metadata = send.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Событие {} успешно отправлено: topic={}, typeEvent={}, partition={}, offset={}",
                    eventType,
                    metadata.topic(),
                    key,
                    metadata.partition(),
                    metadata.offset()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Прервана отправка события {}: topic={}, id={}",
                    eventType, topic, key, e);

        } catch (TimeoutException e) {
            log.error("Таймаут ожидания Kafka: topic={}, id={}",
                    topic, key, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Ошибка при отправке события {}: topic={}, id={}",
                    eventType, topic, key, cause);
        }
    }

    @Override
    public void close() throws Exception {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}
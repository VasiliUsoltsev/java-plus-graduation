package ru.practicum.main.stats.analyzer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionBatchService {

    private static final String UPSERT_SQL = """
            INSERT INTO interactions (user_id, event_id, rating, ts)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (user_id, event_id)
            DO UPDATE SET rating = GREATEST(interactions.rating, EXCLUDED.rating),
                          ts     = EXCLUDED.ts
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void upsertBatch(List<UserActionAvro> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(UPSERT_SQL, batch, batch.size(), (ps, action) -> {
            ps.setLong(1, action.getUserId());
            ps.setLong(2, action.getEventId());
            ps.setDouble(3, getRating(action.getActionType()));
            ps.setTimestamp(4, Timestamp.from(action.getTimestamp()));
        });

        log.debug("Батч действий пользователей сохранён: {} записей", batch.size());
    }

    private double getRating(ActionTypeAvro actionType) {
        return switch (actionType) {
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
            case VIEW -> 0.4;
        };
    }
}
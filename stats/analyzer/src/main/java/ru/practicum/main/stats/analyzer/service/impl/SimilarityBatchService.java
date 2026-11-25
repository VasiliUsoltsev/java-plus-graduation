package ru.practicum.main.stats.analyzer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityBatchService {

    private static final String UPSERT_SQL = """
            INSERT INTO similarities ("eventA", "eventB", similarity, ts)
            VALUES (?, ?, ?, ?)
            ON CONFLICT ("eventA", "eventB")
            DO UPDATE SET similarity = EXCLUDED.similarity,
                          ts         = EXCLUDED.ts
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void upsertBatch(List<EventSimilarityAvro> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(UPSERT_SQL, batch, batch.size(), (ps, sim) -> {
            ps.setLong(1, sim.getEventA());
            ps.setLong(2, sim.getEventB());
            ps.setDouble(3, sim.getScore());
            ps.setTimestamp(4, Timestamp.from(sim.getTimestamp()));
        });

        log.debug("Батч сходств сохранён: {} записей", batch.size());
    }
}
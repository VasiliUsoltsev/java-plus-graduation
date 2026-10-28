package ru.practicum.stat.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.ViewStats;
import ru.practicum.stat.model.Hit;
import ru.practicum.stat.repository.StatsRepository;
import ru.practicum.stat.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository repository;

    @Override
    @Transactional
    public void saveHit(EndpointHit hitDto) {
        log.info("saveHit - начало: app={}, uri={}, ip={}, timestamp={}",
                hitDto.getApp(), hitDto.getUri(), hitDto.getIp(), hitDto.getTimestamp());

        Hit entity = Hit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(hitDto.getTimestamp())
                .build();

        Hit saved = repository.save(entity);
        log.info("saveHit - успешно сохранён: id={}", saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> list, Boolean unique) {
        log.info("getStats - начало: start={}, end={}, uris={}, unique={}",
                start, end, list, unique);

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start must be before or equal to end");
        }

        List<ViewStats> result;
        List<String> uris = list;
        boolean hasUris = uris != null && !uris.isEmpty();
        log.info("Массив URIS проверяем: кол-во записей={}, данные={}",
                hasUris ? uris.size() : 0, uris);

        if (unique && hasUris) {
            result = repository.getStatsWithUniqueIpAndUris(start, end, uris);
        } else if (unique) {
            result = repository.getStatsWithUniqueIpWithoutUris(start, end);
        } else if (hasUris) {
            result = repository.getStatsAllAndUris(start, end, uris);
        } else {
            result = repository.getStatsAllWithoutUris(start, end);
        }

        log.info("getStats - найдено {} записей", result.size());
        return result;
    }
}
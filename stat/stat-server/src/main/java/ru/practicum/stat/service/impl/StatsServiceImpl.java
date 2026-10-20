package ru.practicum.stat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.StatsRequest;
import ru.practicum.stat.dto.ViewStats;
import ru.practicum.stat.model.Hit;
import ru.practicum.stat.repository.StatsRepository;
import ru.practicum.stat.service.StatsService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHit hitDto) {
        Hit hit = Hit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(hitDto.getTimestamp())
                .build();
        statsRepository.save(hit);
    }

    @Override
    public List<ViewStats> getStats(StatsRequest request) {

        if (Boolean.TRUE.equals(request.getUnique())) {
            if (request.getUris() != null && !request.getUris().isEmpty()) {
                return statsRepository.getStatsWithUniqueIpAndUris(request.getStart(), request.getEnd(), request.getUris());
            } else {
                return statsRepository.getStatsWithUniqueIpWithoutUris(request.getStart(), request.getEnd());
            }
        } else {
            if (request.getUris() != null && !request.getUris().isEmpty()) {
                return statsRepository.getStatsAllAndUris(request.getStart(), request.getEnd(), request.getUris());
            } else {
                return statsRepository.getStatsAllWithoutUris(request.getStart(), request.getEnd());
            }
        }
    }
}

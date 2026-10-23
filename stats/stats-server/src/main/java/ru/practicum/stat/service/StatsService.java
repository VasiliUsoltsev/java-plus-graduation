package ru.practicum.stat.service;

import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.StatsRequest;
import ru.practicum.stat.dto.ViewStats;

import java.util.List;

public interface StatsService {
    void saveHit(EndpointHit hit);

    List<ViewStats> getStats(StatsRequest request);
}

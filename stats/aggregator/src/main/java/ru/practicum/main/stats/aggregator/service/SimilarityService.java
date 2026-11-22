package ru.practicum.main.stats.aggregator.service;

import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.List;

public interface SimilarityService {
    List<EventSimilarityAvro> processUserAction(UserActionAvro action);
}

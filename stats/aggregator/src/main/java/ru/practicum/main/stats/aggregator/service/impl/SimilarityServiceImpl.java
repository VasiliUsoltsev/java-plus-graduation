package ru.practicum.main.stats.aggregator.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.main.stats.aggregator.service.SimilarityService;
import ru.practicum.main.stats.aggregator.state.SimilarityStateManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityServiceImpl implements SimilarityService {

    private static final double WEIGHT_VIEW = 0.4;
    private static final double WEIGHT_REGISTER = 0.8;
    private static final double WEIGHT_LIKE = 1.0;

    private final SimilarityStateManager state;

    @Override
    public List<EventSimilarityAvro> processUserAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double newWeight = weightOf(action.getActionType());
        double oldWeight = state.getUserWeight(userId, eventId);

        if (newWeight <= oldWeight) {
            log.debug("Вес не увеличился: user={}, event={}, old={}, new={}. Пересчёт не требуется.",
                    userId, eventId, oldWeight, newWeight);
            return List.of();
        }

        log.info("Обновление веса: user={}, event={}, old={}, new={}", userId, eventId, oldWeight, newWeight);

        state.putUserWeight(userId, eventId, newWeight);
        state.addTotalEventWeight(eventId, newWeight - oldWeight);

        double totalWeightA = state.getTotalEventWeight(eventId);
        long timestampMs = action.getTimestamp().toEpochMilli();

        List<EventSimilarityAvro> result = new ArrayList<>();

        for (Map.Entry<Long, Double> entry : state.getUserInteractions(userId).entrySet()) {
            long otherEventId = entry.getKey();
            if (otherEventId == eventId) {
                continue;
            }

            double otherWeight = entry.getValue();
            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            double deltaMin = newMin - oldMin;

            long first = Math.min(eventId, otherEventId);
            long second = Math.max(eventId, otherEventId);

            if (deltaMin != 0) {
                state.addMinWeight(first, second, deltaMin);
            }

            double totalWeightB = state.getTotalEventWeight(otherEventId);
            if (totalWeightA <= 0 || totalWeightB <= 0) {
                continue;
            }

            double sMin = state.getMinWeight(first, second);
            double similarity = sMin / (Math.sqrt(totalWeightA) * Math.sqrt(totalWeightB));

            result.add(EventSimilarityAvro.newBuilder()
                    .setEventA(first)
                    .setEventB(second)
                    .setScore(similarity)
                    .setTimestamp(Instant.ofEpochMilli(timestampMs))
                    .build());
        }

        return result;
    }

    private double weightOf(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> WEIGHT_VIEW;
            case REGISTER -> WEIGHT_REGISTER;
            case LIKE -> WEIGHT_LIKE;
        };
    }
}
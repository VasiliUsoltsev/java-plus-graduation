package ru.practicum.main.stats.aggregator.state;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimilarityStateManager {

    private final Map<Long, Map<Long, Double>> userWeightsByEvent = new ConcurrentHashMap<>();
    private final Map<Long, Double> totalEventWeights = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    public double getUserWeight(long userId, long eventId) {
        Map<Long, Double> users = userWeightsByEvent.get(eventId);
        if (users == null) {
            return 0.0;
        }
        return users.getOrDefault(userId, 0.0);
    }

    public void putUserWeight(long userId, long eventId, double weight) {
        userWeightsByEvent
                .computeIfAbsent(eventId, k -> new ConcurrentHashMap<>())
                .put(userId, weight);
    }

    public Set<Long> getAllEventIds() {
        return new HashSet<>(totalEventWeights.keySet());
    }

    public void addTotalEventWeight(long eventId, double delta) {
        totalEventWeights.merge(eventId, delta, Double::sum);
    }

    public double getTotalEventWeight(long eventId) {
        return totalEventWeights.getOrDefault(eventId, 0.0);
    }

    public void addMinWeight(long first, long second, double delta) {
        minWeightsSums
                .computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);
    }

    public double getMinWeight(long first, long second) {
        Map<Long, Double> pairs = minWeightsSums.get(first);
        if (pairs == null) {
            return 0.0;
        }
        return pairs.getOrDefault(second, 0.0);
    }
}
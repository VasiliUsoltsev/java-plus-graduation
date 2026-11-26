package ru.practicum.main.stats.analyzer.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.grpc.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.UserPredictionsRequestProto;
import ru.practicum.main.stats.analyzer.model.Interaction;
import ru.practicum.main.stats.analyzer.model.Similarity;
import ru.practicum.main.stats.analyzer.repository.InteractionRepository;
import ru.practicum.main.stats.analyzer.repository.SimilarityRepository;
import ru.practicum.main.stats.analyzer.service.AnalyzerService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyzerServiceImpl implements AnalyzerService {
    private static final int RECENT_LIMIT = 10;
    private static final int NEIGHBOR_LIMIT = 5;

    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        if (maxResults <= 0) {
            return List.of();
        }

        // Подбор кандидатов

        // Последние N мероприятий пользователя
        List<Interaction> recent = interactionRepository
                .findRecentByUserId(userId, PageRequest.of(0, RECENT_LIMIT));
        if (recent.isEmpty()) {
            return List.of();   // пользователь ничего не смотрел — рекомендовать нечего
        }

        List<Long> recentEventIds = recent.stream()
                .map(Interaction::getEventId)
                .toList();

        // Похожие на них (все пары, где участвует любое из recent)
        List<Similarity> similarities = similarityRepository.findAllByEventIds(recentEventIds);

        // Все мероприятия, с которыми пользователь уже взаимодействовал — исключаем из кандидатов
        Set<Long> interacted = new HashSet<>(interactionRepository.findEventIdsByUserId(userId));

        // Веса пользователя по всем мероприятиям — один запрос вместо findById в цикле
        Map<Long, Double> userWeights = interactionRepository.findRatingsByUserId(userId).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).doubleValue()
                ));

        // Кандидаты - второй участник пары, не просмотренный пользователем.
        //    score кандидата = максимальный коэффициент подобия среди всех пар, через которые он попал.
        Map<Long, Double> candidateScores = new HashMap<>();
        for (Similarity s : similarities) {
            long candidate = recentEventIds.contains(s.getEventA()) ? s.getEventB() : s.getEventA();
            if (interacted.contains(candidate)) {
                continue;
            }
            candidateScores.merge(candidate, s.getScore(), Math::max);
        }

        List<Long> candidates = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        // Соседи для всех кандидатов — один запрос вместо findNeighbors в цикле
        List<Similarity> allNeighbors = similarityRepository
                .findNeighborsForCandidates(candidates, new ArrayList<>(interacted));

        Map<Long, List<Similarity>> neighborsByCandidate = new HashMap<>();
        for (Similarity s : allNeighbors) {
            if (candidates.contains(s.getEventA())) {
                neighborsByCandidate.computeIfAbsent(s.getEventA(), k -> new ArrayList<>()).add(s);
            }
            if (candidates.contains(s.getEventB())) {
                neighborsByCandidate.computeIfAbsent(s.getEventB(), k -> new ArrayList<>()).add(s);
            }
        }

        // Предсказание оценки для каждого кандидата

        List<RecommendedEventProto> result = new ArrayList<>();
        for (long candidate : candidates) {
            // K ближайших соседей среди мероприятий пользователя
            List<Similarity> neighbors = neighborsByCandidate.getOrDefault(candidate, List.of()).stream()
                    .sorted(Comparator.comparingDouble(Similarity::getScore).reversed())
                    .limit(NEIGHBOR_LIMIT)
                    .toList();
            if (neighbors.isEmpty()) {
                continue;
            }

            double weightedSum = 0.0;
            double simSum = 0.0;
            for (Similarity s : neighbors) {
                long neighbor = s.getEventA() == candidate ? s.getEventB() : s.getEventA();
                double weight = userWeights.getOrDefault(neighbor, 0.0);
                weightedSum += s.getScore() * weight;
                simSum += s.getScore();
            }
            if (simSum == 0.0) {
                continue;
            }

            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(candidate)
                    .setScore(weightedSum / simSum)
                    .build());
        }

        return result;
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        long eventId = request.getEventId();
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        if (maxResults <= 0) {
            return List.of();
        }

        List<Similarity> similarities = similarityRepository.findAllByEventId(eventId);

        Set<Long> interactedEventIds = new HashSet<>(
                interactionRepository.findEventIdsByUserId(userId));

        return similarities.stream()
                .map(s -> {
                    long candidate = s.getEventA() == eventId ? s.getEventB() : s.getEventA();
                    return Map.entry(candidate, s.getScore());
                })
                .filter(e -> !interactedEventIds.contains(e.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getKey())
                        .setScore(e.getValue())
                        .build())
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        List<Long> eventIds = request.getEventIdsList();
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        List<Object[]> list = interactionRepository.sumRatingsByEventIds(eventIds);

        return list.stream()
                .map(row -> RecommendedEventProto.newBuilder()
                        .setEventId(((Number) row[0]).longValue())
                        .setScore(((Number) row[1]).doubleValue())
                        .build())
                .toList();
    }
}

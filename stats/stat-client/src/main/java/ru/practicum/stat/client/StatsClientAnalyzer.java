package ru.practicum.stat.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.grpc.*;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClientAnalyzer {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, Integer maxResults) {
        Iterator<RecommendedEventProto> iterator = null;
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        try {
            iterator = analyzerStub.getRecommendationsForUser(request);
        } catch (Exception e) {
            log.error("Ошибка при вызове GRPC метода getRecommendationsForUser - ClientAnalizer: {}", e.getMessage());
        }

        return asStream(iterator).toList();
    }

    public List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        Iterator<RecommendedEventProto> iterator = null;
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        try {
            iterator = analyzerStub.getSimilarEvents(request);
        } catch (Exception e) {
            log.error("Ошибка при вызове GRPC метода getSimilarEvents - ClientAnalizer: {}", e.getMessage());
        }

        return asStream(iterator).toList();
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        Iterator<RecommendedEventProto> iterator = null;
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventIds(eventIds)
                .build();

        try {
            iterator = analyzerStub.getInteractionsCount(request);
        } catch (Exception e) {
            log.error("Ошибка при вызове GRPC метода getInteractionsCount - ClientAnalizer: {}", e.getMessage());
        }

        return asStream(iterator).toList();
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}

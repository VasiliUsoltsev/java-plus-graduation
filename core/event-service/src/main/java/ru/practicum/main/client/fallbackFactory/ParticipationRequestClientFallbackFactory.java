package ru.practicum.main.client.fallbackFactory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.main.client.ParticipationRequestClient;
import ru.practicum.main.dto.client.ParticipationRequestDto;
import ru.practicum.main.dto.client.ParticipationStatus;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.exception.model.RequestServiceUnavailableException;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class ParticipationRequestClientFallbackFactory implements FallbackFactory<ParticipationRequestClient> {
    @Override
    public ParticipationRequestClient create(Throwable cause) {
        return new ParticipationRequestClient() {

            @Override
            public List<ParticipationRequestDto> findAllByEventId(Long eventId) {
                if (isNotFound(cause)) {
                    throw new NotFoundException(
                            "Participation requests for event id=" + eventId + " not found");
                }
                log.warn("request-service недоступен: findAllByEventId(eventId={})", eventId, cause);
                throw new RequestServiceUnavailableException(eventId, cause);
            }

            @Override
            public List<ParticipationRequestDto> findAllByIdIn(Collection<Long> ids) {
                log.warn("request-service недоступен: findAllByIdIn(ids={})", ids, cause);
                throw new RequestServiceUnavailableException(cause);
            }

            @Override
            public Long countByEventAndStatus(Long eventId, ParticipationStatus status) {
                if (isNotFound(cause)) {
                    throw new NotFoundException(
                            "Event with id=" + eventId + " was not found");
                }
                log.warn("request-service недоступен: countByEventAndStatus(eventId={}, status={})",
                        eventId, status, cause);
                throw new RequestServiceUnavailableException(eventId, cause);
            }

            @Override
            public List<Object[]> countByEventIdsAndStatus(List<Long> eventIds,
                                                           ParticipationStatus status) {
                log.warn("request-service недоступен: countByEventIdsAndStatus(eventIds={}, status={})",
                        eventIds, status, cause);
                throw new RequestServiceUnavailableException(cause);
            }

            @Override
            public Integer rejectRemainingPending(Long eventId, List<Long> excludeIds) {
                log.warn("request-service недоступен: rejectRemainingPending(eventId={})", eventId, cause);
                throw new RequestServiceUnavailableException(eventId, cause);
            }

            @Override
            public Integer confirmRequestsAtomically(List<Long> ids, Long eventId, int limit) {
                log.warn("request-service недоступен: confirmRequestsAtomically(eventId={})", eventId, cause);
                throw new RequestServiceUnavailableException(eventId, cause);
            }

            private boolean isNotFound(Throwable cause) {
                return cause instanceof feign.FeignException.NotFound;
            }
        };
    }
}

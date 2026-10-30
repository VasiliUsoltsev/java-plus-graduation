package ru.practicum.main.request.service;

import ru.practicum.main.request.dto.response.ParticipationRequestDto;
import ru.practicum.main.request.model.enums.ParticipationStatus;

import java.util.Collection;
import java.util.List;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> findAllByEventId(Long eventId);

    List<ParticipationRequestDto> findAllByIdIn(Collection<Long> ids);

    long countByEventAndStatus(Long eventId, ParticipationStatus status);

    List<Object[]> countByEventIdsAndStatus(List<Long> eventIds, ParticipationStatus status);

    int rejectRemainingPending(Long eventId, List<Long> excludeIds);

    int confirmRequestsAtomically(List<Long> ids, Long eventId, int limit);
}
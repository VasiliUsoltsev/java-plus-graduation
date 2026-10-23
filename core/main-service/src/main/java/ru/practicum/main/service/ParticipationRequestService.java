package ru.practicum.main.service;

import ru.practicum.main.dto.response.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);
}
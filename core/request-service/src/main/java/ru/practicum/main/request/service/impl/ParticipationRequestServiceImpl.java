package ru.practicum.main.request.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.request.client.EventClient;
import ru.practicum.main.request.client.UserClient;
import ru.practicum.main.request.dto.client.EventDto;
import ru.practicum.main.request.dto.client.EventState;
import ru.practicum.main.request.dto.response.ParticipationRequestDto;
import ru.practicum.main.request.exception.model.ConflictException;
import ru.practicum.main.request.exception.model.EventServiceUnavailableException;
import ru.practicum.main.request.exception.model.NotFoundException;
import ru.practicum.main.request.exception.model.UserServiceUnavailableException;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.enums.ParticipationStatus;
import ru.practicum.main.request.repository.ParticipationRequestRepository;
import ru.practicum.main.request.service.ParticipationRequestService;
import ru.practicum.main.request.service.mapper.ParticipationRequestMapper;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventClient eventClient;
    private final UserClient userClient;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        if (!existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
        List<ParticipationRequest> requests = requestRepository.findAllByRequester(userId);
        return requests.stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        EventDto event = findEventById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event is not published");
        }
        if (event.getInitiator() != null && event.getInitiator().id().equals(userId)) {
            throw new ConflictException("Initiator cannot participate in their own event");
        }
        if (requestRepository.findByEventAndRequester(eventId, userId).isPresent()) {
            throw new ConflictException("Duplicate request for this event");
        }
        long confirmed = requestRepository.countByEventAndStatus(eventId, ParticipationStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit has been reached");
        }
        ParticipationStatus initialStatus = (event.getParticipantLimit() == 0 || !event.getRequestModeration())
                ? ParticipationStatus.CONFIRMED
                : ParticipationStatus.PENDING;

        ParticipationRequest request = ParticipationRequest.builder()
                .event(eventId)
                .requester(userId)
                .status(initialStatus)
                .build();

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Created request {} for event {} by user {}", saved.getId(), eventId, userId);
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " not found"));
        if (!request.getRequester().equals(userId)) {
            throw new NotFoundException("Request with id=" + requestId + " not found for this user");
        }
        request.setStatus(ParticipationStatus.CANCELED);
        ParticipationRequest updated = requestRepository.save(request);
        log.info("Cancelled request {} by user {}", requestId, userId);
        return ParticipationRequestMapper.toDto(updated);
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventId(Long eventId) {
        return requestRepository.findAllByEvent(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    public List<ParticipationRequestDto> findAllByIdIn(Collection<Long> ids) {
        return requestRepository.findAllByIdIn(ids).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    public long countByEventAndStatus(Long eventId, ParticipationStatus status) {
        return requestRepository.countByEventAndStatus(eventId, status);
    }

    @Override
    public List<Object[]> countByEventIdsAndStatus(List<Long> eventIds, ParticipationStatus status) {
        return requestRepository.countByEventIdsAndStatus(eventIds, status);
    }

    @Override
    @Transactional
    public int rejectRemainingPending(Long eventId, List<Long> excludeIds) {
        return requestRepository.rejectRemainingPending(eventId, excludeIds);
    }

    @Override
    @Transactional
    public int confirmRequestsAtomically(List<Long> ids, Long eventId, int limit) {
        return requestRepository.confirmRequestsAtomically(ids, eventId, limit);
    }

    private Boolean existsById(Long userId) {
        try {
            return userClient.existsById(userId);
        } catch (FeignException exception) {
            throw new UserServiceUnavailableException(userId, exception);
        }
    }

    private EventDto findEventById(Long eventId) {
        try {
            return eventClient.findById(eventId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("Event with id=" + eventId + " not found");
        } catch (FeignException exception) {
            throw new EventServiceUnavailableException(eventId, exception);
        }
    }
}
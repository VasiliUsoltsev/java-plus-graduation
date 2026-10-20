package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.enums.AdminStateAction;
import ru.practicum.main.dto.enums.EventSort;
import ru.practicum.main.dto.enums.EventStateAction;
import ru.practicum.main.dto.enums.RequestStatus;
import ru.practicum.main.dto.request.*;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.dto.response.ParticipationRequestDto;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.ParticipationRequest;
import ru.practicum.main.model.User;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.model.enums.ParticipationStatus;
import ru.practicum.main.repository.*;
import ru.practicum.main.service.EventService;
import ru.practicum.main.service.mapper.EventMapper;
import ru.practicum.main.service.mapper.ParticipationRequestMapper;
import ru.practicum.stat.client.StatClient;
import ru.practicum.stat.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private static final String EVENT_NOT_FOUND_EXCEPTION = "Event with id=%d was not found";
    private static final String USER_NOT_FOUND_EXCEPTION = "User with id=%d was not found";
    private static final String CATEGORY_NOT_FOUND_EXCEPTION = "Category with id=%d was not found";
    private static final String EVENT_DATE_IN_PAST_EXCEPTION =
            "Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s";
    private static final String EVENT_MUST_NOT_BE_PUBLISHED = "Event must not be published";
    private static final String EVENT_MUST_NOT_HAVE_PENDING_REQUESTS = "Request must have status PENDING";
    private static final String CANNOT_PUBLISH =
            "Cannot publish the event because it's not in the right state";
    private static final String CANNOT_REJECT =
            "Cannot reject the event because it's already published";
    private static final String REQUEST_IDS_NOT_FOUND = "One or more request IDs were not found or do not belong to this event";

    private static final DateTimeFormatter STAT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventRepository eventRepository;
    private final EventSearchRepository eventSearchRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatClient statClient;
    private final EventCommentRepository eventCommentRepository;

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> page = eventRepository.findAllByInitiatorId(userId, pageable);

        List<Event> events = page.getContent();
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedMap = getConfirmedRequestsBatch(eventIds);
        Map<Long, Long> viewsMap = getViewsBatch(events);
        Map<Long, Long> countEventCommentsMap = countCommentsByEventId(eventIds);

        return events.stream()
                .map(event -> EventMapper.toShortDto(event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L),
                        countEventCommentsMap.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEvent) {
        User initiator = getUser(userId);
        Category category = getCategory(newEvent.getCategory());

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        if (newEvent.getEventDate() == null || newEvent.getEventDate().isBefore(now.plusHours(2))) {
            throw new IllegalArgumentException(String.format(EVENT_DATE_IN_PAST_EXCEPTION, newEvent.getEventDate()));
        }

        Event event = EventMapper.toEntity(newEvent, initiator, category);
        event.setCreatedOn(now);
        event.setState(EventState.PENDING);

        event = eventRepository.save(event);

        return EventMapper.toFullDto(event, 0L, 0L, 0L);
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = getUserEventOrThrow(userId, eventId);
        return EventMapper.toFullDto(
                event,
                getConfirmedRequests(eventId),
                getViews(event),
                countCommentByEventId(eventId)
        );
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest update) {
        Event event = getUserEventOrThrow(userId, eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException(EVENT_MUST_NOT_BE_PUBLISHED);
        }

        if (update.getEventDate() != null && update.getEventDate().isBefore(LocalDateTime.now(ZoneId.systemDefault()).plusHours(2))) {
            throw new IllegalArgumentException(String.format(EVENT_DATE_IN_PAST_EXCEPTION, update.getEventDate()));
        }

        applyUpdate(event, update);

        if (update.getStateAction() != null) {
            if (update.getStateAction() == EventStateAction.SEND_TO_REVIEW) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException(EVENT_MUST_NOT_BE_PUBLISHED);
                }
                event.setState(EventState.PENDING);
            } else if (update.getStateAction() == EventStateAction.CANCEL_REVIEW) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot cancel review: event must be in PENDING state");
                }
                event.setState(EventState.CANCELED);
            }
        }

        event = eventRepository.save(event);

        return EventMapper.toFullDto(
                event,
                getConfirmedRequests(eventId),
                getViews(event),
                countCommentByEventId(eventId)
        );
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        getUserEventOrThrow(userId, eventId);
        return participationRequestRepository.findAllByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        Event event = getUserEventOrThrow(userId, eventId);

        List<ParticipationRequest> requests = participationRequestRepository
                .findAllByIdIn(request.getRequestIds());
        if (requests.size() != request.getRequestIds().size()) {
            throw new NotFoundException(REQUEST_IDS_NOT_FOUND);
        }
        if (requests.stream().anyMatch(r -> !r.getEvent().getId().equals(eventId))) {
            throw new NotFoundException(REQUEST_IDS_NOT_FOUND);
        }
        if (requests.stream().anyMatch(r -> r.getStatus() != ParticipationStatus.PENDING)) {
            throw new ConflictException(EVENT_MUST_NOT_HAVE_PENDING_REQUESTS);
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (request.getStatus() == RequestStatus.REJECTED) {
            for (ParticipationRequest r : requests) {
                r.setStatus(ParticipationStatus.REJECTED);
                rejected.add(ParticipationRequestMapper.toDto(r));
            }
            return new EventRequestStatusUpdateResult(confirmed, rejected);
        }

        if (event.getParticipantLimit() > 0
                && participationRequestRepository
                .countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        participationRequestRepository.confirmRequestsAtomically(
                request.getRequestIds(), eventId, event.getParticipantLimit());

        long confirmedTotal = participationRequestRepository
                .countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);

        if (event.getParticipantLimit() > 0 && confirmedTotal >= event.getParticipantLimit()) {
            List<Long> confirmedIds = participationRequestRepository
                    .findAllByIdIn(request.getRequestIds()).stream()
                    .filter(r -> r.getStatus() == ParticipationStatus.CONFIRMED)
                    .map(ParticipationRequest::getId)
                    .toList();
            participationRequestRepository.rejectRemainingPending(eventId, confirmedIds);
        }

        List<ParticipationRequest> allAffected = participationRequestRepository
                .findAllByIdIn(request.getRequestIds());

        for (ParticipationRequest r : allAffected) {
            if (r.getStatus() == ParticipationStatus.CONFIRMED) {
                confirmed.add(ParticipationRequestMapper.toDto(r));
            } else {
                rejected.add(ParticipationRequestMapper.toDto(r));
            }
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public List<EventFullDto> searchEventsAdmin(AdminEventSearchParams params) {
        LocalDateTime start = parseDateTimeOr(params.getRangeStart(), null);
        LocalDateTime end = parseDateTimeOr(params.getRangeEnd(), null);
        validateRange(start, end);
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        List<EventState> eventStates = null;
        if (params.getStates() != null && !params.getStates().isEmpty()) {
            eventStates = params.getStates().stream()
                    .map(EventState::from)
                    .toList();
        }

        AdminEventSearchFilter filter = new AdminEventSearchFilter(
                params.getUsers(), eventStates, params.getCategories(), start, end);

        Page<Event> page = eventSearchRepository.searchAdmin(filter, pageable);

        List<Event> events = page.getContent();
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedMap = getConfirmedRequestsBatch(eventIds);
        Map<Long, Long> viewsMap = getViewsBatch(events);
        Map<Long, Long> countEventCommentsMap = countCommentsByEventId(eventIds);

        return events.stream()
                .map(event -> EventMapper.toFullDto(event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L),
                        countEventCommentsMap.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest update) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND_EXCEPTION, eventId)));

        if (update.getEventDate() != null
                && update.getEventDate().isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw new IllegalArgumentException(String.format(EVENT_DATE_IN_PAST_EXCEPTION, update.getEventDate()));
        }

        if (update.getEventDate() != null
                && event.getPublishedOn() != null
                && update.getEventDate().isBefore(event.getPublishedOn().plusHours(1))) {
            throw new ConflictException(String.format(EVENT_DATE_IN_PAST_EXCEPTION, update.getEventDate()));
        }

        applyUpdate(event, update);

        if (update.getStateAction() != null) {
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            if (update.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException(CANNOT_PUBLISH);
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(now);
            } else if (update.getStateAction() == AdminStateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException(CANNOT_REJECT);
                }
                event.setState(EventState.CANCELED);
            }
        }

        event = eventRepository.save(event);

        return EventMapper.toFullDto(
                event,
                getConfirmedRequests(eventId),
                getViews(event),
                countCommentByEventId(eventId)
        );
    }

    @Override
    public List<EventShortDto> searchPublicEvents(PublicEventSearchParams params, String ip) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime start = parseDateTimeOr(params.getRangeStart(), now);
        LocalDateTime end = parseDateTimeOr(params.getRangeEnd(), null);
        validateRange(start, end);
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                params.getText(), EventState.PUBLISHED, params.getCategories(),
                params.getPaid(), start, end, params.getOnlyAvailable());

        Page<Event> page = eventSearchRepository.searchPublic(filter, pageable);

        List<Event> events = page.getContent();
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedMap = getConfirmedRequestsBatch(eventIds);
        Map<Long, Long> viewsMap = getViewsBatch(events);
        Map<Long, Long> countEventCommentsMap = countCommentsByEventId(eventIds);

        List<EventShortDto> result = events.stream()
                .map(event -> EventMapper.toShortDto(event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L),
                        countEventCommentsMap.getOrDefault(event.getId(), 0L)))
                .toList();

        if (EventSort.VIEWS == params.getSort()) {
            result = result.stream()
                    .sorted(Comparator.comparing(EventShortDto::getViews,
                                    Comparator.nullsLast(Comparator.naturalOrder()))
                            .reversed())
                    .toList();
        }

        statClient.saveHit(ip, StatClient.EVENT_URI);

        return result;
    }

    @Override
    public EventFullDto getPublishedEventById(Long eventId, String ip) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND_EXCEPTION, eventId)));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(String.format(EVENT_NOT_FOUND_EXCEPTION, eventId));
        }

        statClient.saveHit(ip, StatClient.EVENT_URI + "/" + eventId);

        return EventMapper.toFullDto(
                event,
                getConfirmedRequests(eventId),
                getViews(event),
                countCommentByEventId(eventId)
        );
    }

    private Event getUserEventOrThrow(Long userId, Long eventId) {
        return eventRepository.findByEventIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND_EXCEPTION, eventId)));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format(USER_NOT_FOUND_EXCEPTION, userId)));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(String.format(CATEGORY_NOT_FOUND_EXCEPTION, categoryId)));
    }

    private long getConfirmedRequests(Long eventId) {
        return participationRequestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);
    }

    private Map<Long, Long> getConfirmedRequestsBatch(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> counts = participationRequestRepository
                .countByEventIdsAndStatus(eventIds, ParticipationStatus.CONFIRMED);
        return counts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private long getViews(Event event) {
        return getViewsBatch(List.of(event)).getOrDefault(event.getId(), 0L);
    }

    private Map<Long, Long> getViewsBatch(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime start = events.stream()
                .map(e -> e.getPublishedOn() != null ? e.getPublishedOn() : e.getCreatedOn())
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(end);
        List<String> uris = events.stream()
                .map(e -> StatClient.EVENT_URI + "/" + e.getId())
                .toList();
        List<ViewStats> stats = statClient.getStats(
                start.format(STAT_DATE_FORMATTER),
                end.format(STAT_DATE_FORMATTER),
                uris,
                true
        );
        if (stats == null || stats.isEmpty()) {
            return Map.of();
        }
        return stats.stream()
                .filter(v -> v.getHits() != null && v.getUri() != null)
                .collect(Collectors.toMap(
                        v -> Long.parseLong(v.getUri().replace(StatClient.EVENT_URI + "/", "")),
                        ViewStats::getHits
                ));
    }

    private Long countCommentByEventId(Long eventId) {
        return eventCommentRepository.countByEventId(eventId);
    }

    private Map<Long, Long> countCommentsByEventId(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        return eventCommentRepository.countByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private LocalDateTime parseDateTimeOr(String dateTime, LocalDateTime defaultValue) {
        if (dateTime == null || dateTime.isBlank()) {
            return defaultValue;
        }
        return LocalDateTime.parse(dateTime, STAT_DATE_FORMATTER);
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Field: rangeStart. Error: должно быть не позже rangeEnd. Value: " + start);
        }
    }

    private void applyUpdate(Event event, UpdateEventBaseRequest update) {
        if (update.getAnnotation() != null) {
            event.setAnnotation(update.getAnnotation());
        }
        if (update.getCategory() != null) {
            event.setCategory(getCategory(update.getCategory()));
        }
        if (update.getDescription() != null) {
            event.setDescription(update.getDescription());
        }
        if (update.getEventDate() != null) {
            event.setEventDate(update.getEventDate());
        }
        if (update.getLocation() != null) {
            event.setLocation(update.getLocation());
        }
        if (update.getPaid() != null) {
            event.setPaid(update.getPaid());
        }
        if (update.getParticipantLimit() != null) {
            event.setParticipantLimit(update.getParticipantLimit());
        }
        if (update.getRequestModeration() != null) {
            event.setRequestModeration(update.getRequestModeration());
        }
        if (update.getTitle() != null) {
            event.setTitle(update.getTitle());
        }
    }
}




package ru.practicum.main.service;

import ru.practicum.main.dto.request.AdminEventSearchParams;
import ru.practicum.main.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.request.NewEventDto;
import ru.practicum.main.dto.request.PublicEventSearchParams;
import ru.practicum.main.dto.request.UpdateEventAdminRequest;
import ru.practicum.main.dto.request.UpdateEventUserRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.dto.response.ParticipationRequestDto;

import java.util.List;

public interface EventService {

    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto addEvent(Long userId, NewEventDto newEvent);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest update);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest request);

    List<EventFullDto> searchEventsAdmin(AdminEventSearchParams params);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest update);

    List<EventShortDto> searchPublicEvents(PublicEventSearchParams params, String ip);

    EventFullDto getPublishedEventById(Long eventId, String ip);
}

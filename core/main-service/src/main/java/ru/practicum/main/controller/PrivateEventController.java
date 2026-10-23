package ru.practicum.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.request.NewEventDto;
import ru.practicum.main.dto.request.UpdateEventUserRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.dto.response.ParticipationRequestDto;
import ru.practicum.main.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/users/{userId}/events")
public class PrivateEventController {
    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEvents(@PathVariable @Positive Long userId,
                                         @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                         @RequestParam(defaultValue = "10") @Positive Integer size) {
        return eventService.getUserEvents(userId, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable @Positive Long userId,
                                 @Valid @RequestBody NewEventDto newEvent) {
        return eventService.addEvent(userId, newEvent);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEvent(@PathVariable @Positive Long userId,
                                 @PathVariable @Positive Long eventId) {
        return eventService.getUserEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long eventId,
                                    @Valid @RequestBody UpdateEventUserRequest update) {
        return eventService.updateUserEvent(userId, eventId, update);
    }

    @GetMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getEventRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {
        return eventService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult changeRequestStatus(@PathVariable @Positive Long userId,
                                                              @PathVariable @Positive Long eventId,
                                                              @Valid @RequestBody
                                                              EventRequestStatusUpdateRequest request) {
        return eventService.changeRequestStatus(userId, eventId, request);
    }
}

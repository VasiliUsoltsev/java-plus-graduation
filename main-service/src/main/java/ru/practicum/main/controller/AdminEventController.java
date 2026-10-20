package ru.practicum.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.AdminEventSearchParams;
import ru.practicum.main.dto.request.UpdateEventAdminRequest;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/admin/events")
public class AdminEventController {
    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> searchEvents(@ModelAttribute @Valid AdminEventSearchParams params) {
        return eventService.searchEventsAdmin(params);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable @Positive Long eventId,
                                    @Valid @RequestBody UpdateEventAdminRequest update) {
        return eventService.updateEventAdmin(eventId, update);
    }
}

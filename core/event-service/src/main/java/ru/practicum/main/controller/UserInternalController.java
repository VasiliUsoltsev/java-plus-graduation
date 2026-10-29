package ru.practicum.main.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j
@RequestMapping("/internal")
public class UserInternalController {
    private final EventService eventService;

    @GetMapping("/events/{eventId}")
    public EventFullDto getEvent(@PathVariable @Positive Long eventId) {
        log.debug("GET - /internal/events/{eventId} - метод getEvent - eventId = " + eventId);
        return eventService.getEvent(eventId);
    }

    @GetMapping("/events/{eventId}/existsevent")
    public Boolean existsEventById(@PathVariable @Positive Long eventId) {
        log.debug("GET - /internal/events/{eventId}/existsevent - метод existsEventById - eventId = " + eventId);
        return eventService.existsEventById(eventId);
    }

    @GetMapping("/eventsbatch")
    public List<Object[]> getEventTitleBatch(@RequestParam List<Long> eventIds){
        return eventService.getEventTitleBatch(eventIds);
    }
}
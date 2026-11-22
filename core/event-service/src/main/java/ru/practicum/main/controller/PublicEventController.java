package ru.practicum.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.PublicEventSearchParams;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/events")
public class PublicEventController {
    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> searchEvents(@ModelAttribute @Valid PublicEventSearchParams params) {
        return eventService.searchPublicEvents(params);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEvent(@PathVariable Long id,
                                 @RequestHeader("X-EWM-USER-ID") Long userId) {
        return eventService.getPublishedEventById(id, userId);
    }

    @GetMapping("/recommendations")
    public List<EventFullDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") Long userId,
                                                 @RequestParam(defaultValue = "10") @Positive Integer maxResults) {
        return eventService.getRecommendations(userId, maxResults);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable Long eventId,
                          @RequestHeader("X-EWM-USER-ID") Long userId) {
        eventService.addLikeEvent(eventId, userId);
    }
}

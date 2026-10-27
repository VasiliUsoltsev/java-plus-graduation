package ru.practicum.main.comment.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.comment.service.EventCommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j
@RequestMapping("/internal")
public class UserInternalController {
    private final EventCommentService eventCommentService;

    @GetMapping("/countEvent/{eventId}")
    public Long countEvent(@PathVariable @Positive Long eventId) {
        return eventCommentService.countByEvent(eventId);
    }

    @GetMapping("/countEvents")
    public List<Object[]> countEvents(@RequestParam List<Long> eventIds) {
        return eventCommentService.countByEventIds(eventIds);
    }
}
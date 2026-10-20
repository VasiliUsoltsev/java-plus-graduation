package ru.practicum.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.*;
import ru.practicum.main.dto.response.EventCommentAuthorDto;
import ru.practicum.main.dto.response.EventCommentUserDto;
import ru.practicum.main.service.EventCommentService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("events/{eventId}/comments")
public class PrivateEventCommentController {
    private final EventCommentService eventCommentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventCommentAuthorDto addComment(@RequestBody @Valid EventCommentDto newComment,
                                            @PathVariable @Positive Long eventId,
                                            @RequestHeader("X-Explore-With-Me-User-Id") Long userId
    ) {
        NewEventCommentParamDto param = NewEventCommentParamDto.builder()
                .eventId(eventId)
                .userId(userId)
                .build();
        return eventCommentService.addComment(newComment, param);
    }

    @PatchMapping("/{commentId}")
    public EventCommentAuthorDto updateComment(@RequestBody @Valid EventCommentDto updateComment,
                                               @PathVariable @Positive Long eventId,
                                               @PathVariable @Positive Long commentId,
                                               @RequestHeader("X-Explore-With-Me-User-Id") @Positive Long userId
    ) {

        EventCommentParamDto param = EventCommentParamDto.builder()
                .eventId(eventId)
                .userId(userId)
                .commentId(commentId)
                .build();

        return eventCommentService.updateComment(updateComment, param);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable @Positive Long commentId,
                              @PathVariable @Positive Long eventId,
                              @RequestHeader("X-Explore-With-Me-User-Id") @Positive Long userId
    ) {
        EventCommentParamDto param = EventCommentParamDto.builder()
                .eventId(eventId)
                .userId(userId)
                .commentId(commentId)
                .build();

        eventCommentService.deleteComment(param);
    }

    @GetMapping
    public List<EventCommentAuthorDto> getCommentsEventByUser(
            @PathVariable @Positive Long eventId,
            @RequestHeader("X-Explore-With-Me-User-Id") @Positive Long userId
    ) {
        EventCommentParamDto param = EventCommentParamDto.builder()
                .eventId(eventId)
                .userId(userId)
                .build();

        return eventCommentService.getCommentsEventByUser(param);
    }

    @GetMapping("/approved")
    public List<EventCommentUserDto> getCommentsEventApproved(
            @PathVariable @Positive Long eventId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size
    ) {
        GetEventCommentParamDto param = GetEventCommentParamDto.builder()
                .from(from)
                .size(size)
                .build();

        return eventCommentService.getCommentsByEvent(eventId, param);
    }
}

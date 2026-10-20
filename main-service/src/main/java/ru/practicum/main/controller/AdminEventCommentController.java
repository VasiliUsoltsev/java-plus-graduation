package ru.practicum.main.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.EventCommentRequestStatus;
import ru.practicum.main.dto.request.GetEventCommentParamDto;
import ru.practicum.main.dto.response.EventCommentAdminDto;
import ru.practicum.main.service.EventCommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("admin/comments")
public class AdminEventCommentController {
    private final EventCommentService eventCommentService;

    @GetMapping("/{commentId}")
    public EventCommentAdminDto getCommentById(@PathVariable @Positive Long commentId) {
        return eventCommentService.getCommentById(commentId);
    }

    @GetMapping
    public List<EventCommentAdminDto> getComments(@RequestParam(defaultValue = "ALL") EventCommentRequestStatus state,
                                                  @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                  @RequestParam(defaultValue = "10") @Positive Integer size
    ) {
        GetEventCommentParamDto param = GetEventCommentParamDto.builder()
                .from(from)
                .size(size)
                .build();

        return eventCommentService.getAllComments(state, param);
    }

    @PatchMapping("/{commentId}/approve")
    public EventCommentAdminDto approve(@PathVariable @Positive Long commentId) {
        return eventCommentService.approve(commentId);
    }

    @PatchMapping("/{commentId}/reject")
    public EventCommentAdminDto reject(@PathVariable @Positive Long commentId) {
        return eventCommentService.reject(commentId);
    }
}
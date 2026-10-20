package ru.practicum.main.service;

import ru.practicum.main.dto.request.*;
import ru.practicum.main.dto.response.EventCommentAdminDto;
import ru.practicum.main.dto.response.EventCommentAuthorDto;
import ru.practicum.main.dto.response.EventCommentUserDto;

import java.util.List;

public interface EventCommentService {
    EventCommentAuthorDto addComment(EventCommentDto newComment, NewEventCommentParamDto param);

    EventCommentAuthorDto updateComment(EventCommentDto updateComment, EventCommentParamDto param);

    void deleteComment(EventCommentParamDto param);

    List<EventCommentUserDto> getCommentsByEvent(Long eventId, GetEventCommentParamDto param);

    List<EventCommentAuthorDto> getCommentsEventByUser(EventCommentParamDto param);

    EventCommentAdminDto approve(Long commentId);

    EventCommentAdminDto reject(Long commentId);

    EventCommentAdminDto getCommentById(Long commentId);

    List<EventCommentAdminDto> getAllComments(EventCommentRequestStatus state, GetEventCommentParamDto param);
}

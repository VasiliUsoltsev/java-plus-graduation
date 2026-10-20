package ru.practicum.main.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.request.EventCommentDto;
import ru.practicum.main.dto.response.EventCommentAdminDto;
import ru.practicum.main.dto.response.EventCommentAuthorDto;
import ru.practicum.main.dto.response.EventCommentUserDto;
import ru.practicum.main.model.EventComment;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventCommentMapper {
    public static EventComment toEventComment(EventCommentDto newComment) {
        return EventComment.builder()
                .content(newComment.getContent())
                .build();
    }

    public static EventCommentAuthorDto toEventCommentAuthorDto(EventComment comment) {
        return EventCommentAuthorDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .event(comment.getEvent().getTitle())
                .author(comment.getAuthor().getName())
                .status(comment.getStatus())
                .created(comment.getCreated())
                .build();
    }

    public static EventCommentUserDto toEventCommentUserDto(EventComment comment) {
        return EventCommentUserDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(comment.getAuthor().getName())
                .created(comment.getCreated())
                .build();
    }

    public static EventCommentAdminDto toEventCommentAdminDto(EventComment comment) {
        return EventCommentAdminDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(UserMapper.toDto(comment.getAuthor()))
                .status(comment.getStatus())
                .created(comment.getCreated())
                .event(comment.getEvent().getTitle())
                .build();
    }
}

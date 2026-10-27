package ru.practicum.main.comment.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.comment.dto.client.UserDto;
import ru.practicum.main.comment.dto.request.EventCommentDto;
import ru.practicum.main.comment.dto.response.EventCommentAdminDto;
import ru.practicum.main.comment.dto.response.EventCommentAuthorDto;
import ru.practicum.main.comment.dto.response.EventCommentUserDto;
import ru.practicum.main.comment.model.EventComment;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventCommentMapper {
    public static EventComment toEventComment(EventCommentDto newComment) {
        return EventComment.builder()
                .content(newComment.getContent())
                .build();
    }

    public static EventCommentAuthorDto toEventCommentAuthorDto(EventComment comment, String title, String author) {
        return EventCommentAuthorDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .event(title)
                .author(author)
                .status(comment.getStatus())
                .created(comment.getCreated())
                .build();
    }

    public static EventCommentUserDto toEventCommentUserDto(EventComment comment, String name) {
        return EventCommentUserDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(name)
                .created(comment.getCreated())
                .build();
    }

    public static EventCommentAdminDto toEventCommentAdminDto(EventComment comment, String title, UserDto author) {
        return EventCommentAdminDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(author)
                .status(comment.getStatus())
                .created(comment.getCreated())
                .event(title)
                .build();
    }
}

package ru.practicum.main.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.model.enums.EventCommentStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCommentAuthorDto {
    private Long id;
    private String content;
    private String event;
    private String author;
    private EventCommentStatus status;
    private LocalDateTime created;
}

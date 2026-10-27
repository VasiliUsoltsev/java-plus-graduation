package ru.practicum.main.comment.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventCommentParamDto {
    private Long userId;
    private Long eventId;
}

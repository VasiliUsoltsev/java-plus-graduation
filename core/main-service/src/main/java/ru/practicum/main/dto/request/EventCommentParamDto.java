package ru.practicum.main.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCommentParamDto {
    private Long userId;
    private Long commentId;
    private Long eventId;
}

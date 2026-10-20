package ru.practicum.main.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetEventCommentParamDto {
    private Integer from;
    private Integer size;
}

package ru.practicum.main.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCommentDto {
    @NotBlank
    @Size(min = 10, max = 5000)
    private String content;
}

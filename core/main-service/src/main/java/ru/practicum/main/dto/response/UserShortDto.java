package ru.practicum.main.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserShortDto {
    private Long id;
    private String name;
}
package ru.practicum.main.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    @NotNull
    private Float lat;

    @NotNull
    private Float lon;
}
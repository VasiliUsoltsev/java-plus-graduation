package ru.practicum.main.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {
    private List<Long> events;

    private Boolean pinned;

    @NotBlank
    @Size(min = 1, max = 50)
    private String title;
}
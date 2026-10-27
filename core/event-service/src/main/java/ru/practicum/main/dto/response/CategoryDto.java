package ru.practicum.main.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;

    @NotBlank(message = "Name must not be blank")
    @Size(min = 1, max = 50, message = "Name length must be between 1 and 50")
    private String name;
}
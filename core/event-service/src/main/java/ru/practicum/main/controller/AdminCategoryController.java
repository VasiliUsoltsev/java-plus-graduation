package ru.practicum.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.NewCategoryDto;
import ru.practicum.main.dto.response.CategoryDto;
import ru.practicum.main.service.CategoryService;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto addCategory(@Valid @RequestBody NewCategoryDto newCategory) {
        return categoryService.addCategory(newCategory);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategoryId(@PathVariable @Positive Long catId) {
        categoryService.deleteCategory(catId);
    }

    @PatchMapping("/{catId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto updateCategory(@PathVariable @Positive Long catId,
                                      @Valid @RequestBody CategoryDto updateCategory) {
        return categoryService.updateCategory(catId, updateCategory);
    }
}

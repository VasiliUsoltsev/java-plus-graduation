package ru.practicum.main.service;

import ru.practicum.main.dto.request.NewCategoryDto;
import ru.practicum.main.dto.response.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto addCategory(NewCategoryDto newCategory);

    CategoryDto updateCategory(Long catId, CategoryDto updateCategory);

    void deleteCategory(Long catId);

    List<CategoryDto> getCategories(Integer from, Integer size);

    CategoryDto getCategory(Long catId);
}

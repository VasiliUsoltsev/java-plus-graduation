package ru.practicum.main.categories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.NewCategoryDto;
import ru.practicum.main.dto.response.CategoryDto;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.Category;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.impl.CategoryServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private NewCategoryDto newCategoryDto;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Концерты")
                .build();

        newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("Концерты");

        categoryDto = new CategoryDto(1L, "Концерты");
    }

    // ==================== ТЕСТЫ ДЛЯ addCategory ====================

    @Test
    void addCategory_ShouldReturnCategoryDto_WhenValid() {
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryDto result = categoryService.addCategory(newCategoryDto);

        assertNotNull(result);
        assertEquals(category.getId(), result.getId());
        assertEquals(category.getName(), result.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void addCategory_ShouldSaveCategory_WithCorrectData() {
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.addCategory(newCategoryDto);

        verify(categoryRepository).save(argThat(savedCategory ->
                savedCategory.getName().equals("Концерты")
        ));
    }

    // ==================== ТЕСТЫ ДЛЯ updateCategory ====================

    @Test
    void updateCategory_ShouldReturnUpdatedCategoryDto_WhenCategoryExists() {
        CategoryDto updateDto = new CategoryDto();
        updateDto.setName("Обновленные концерты");

        Category updatedCategory = Category.builder()
                .id(1L)
                .name("Обновленные концерты")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        CategoryDto result = categoryService.updateCategory(1L, updateDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Обновленные концерты", result.getName());
        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void updateCategory_ShouldThrowNotFoundException_WhenCategoryDoesNotExist() {
        CategoryDto updateDto = new CategoryDto();
        updateDto.setName("Обновленные концерты");

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.updateCategory(999L, updateDto));

        assertEquals("Category was not found with id=999", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ==================== ТЕСТЫ ДЛЯ deleteCategory ====================

    @Test
    void deleteCategory_ShouldDelete_WhenCategoryExists() {
        Long categoryId = 1L;
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Test Category");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(categoryId)).thenReturn(false);
        doNothing().when(categoryRepository).delete(category); // или deleteById

        assertDoesNotThrow(() -> categoryService.deleteCategory(categoryId));

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(eventRepository, times(1)).existsByCategoryId(categoryId);
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    void deleteCategory_ShouldThrowNotFoundException_WhenCategoryDoesNotExist() {
        Long categoryId = 999L;

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.deleteCategory(categoryId));

        assertEquals("Category was not found with id=999", exception.getMessage());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(eventRepository, never()).existsByCategoryId(anyLong());
        verify(categoryRepository, never()).delete(any());
    }

    // ==================== ТЕСТЫ ДЛЯ getCategories ====================

    @Test
    void getCategories_ShouldReturnListOfCategories_WhenValidParams() {
        List<Category> categories = List.of(category);
        Page<Category> page = new PageImpl<>(categories);

        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

        List<CategoryDto> result = categoryService.getCategories(0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Концерты", result.get(0).getName());

        verify(categoryRepository, times(1)).findAll(PageRequest.of(0, 10));
    }

    @Test
    void getCategories_ShouldReturnEmptyList_WhenNoCategories() {
        Page<Category> emptyPage = new PageImpl<>(List.of());

        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        List<CategoryDto> result = categoryService.getCategories(0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository, times(1)).findAll(PageRequest.of(0, 10));
    }

    @Test
    void getCategories_ShouldCalculatePageNumberCorrectly() {
        List<Category> categories = List.of(category);
        Page<Category> page = new PageImpl<>(categories);

        when(categoryRepository.findAll(PageRequest.of(2, 10))).thenReturn(page);

        categoryService.getCategories(20, 10);

        verify(categoryRepository).findAll(PageRequest.of(2, 10));
    }

    @Test
    void getCategories_ShouldHandleLargeFromValue() {
        List<Category> categories = List.of(category);
        Page<Category> page = new PageImpl<>(categories);

        when(categoryRepository.findAll(PageRequest.of(3, 5))).thenReturn(page);

        categoryService.getCategories(15, 5);

        verify(categoryRepository).findAll(PageRequest.of(3, 5));
    }

    // ==================== ТЕСТЫ ДЛЯ getCategory ====================

    @Test
    void getCategory_ShouldReturnCategoryDto_WhenExists() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryDto result = categoryService.getCategory(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Концерты", result.getName());
        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    void getCategory_ShouldThrowNotFoundException_WhenCategoryDoesNotExist() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.getCategory(999L));

        assertEquals("Category was not found with id=999", exception.getMessage());
        verify(categoryRepository, times(1)).findById(999L);
    }
}

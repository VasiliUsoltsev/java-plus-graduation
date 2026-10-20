package ru.practicum.main.categories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.controller.PublicCategoryController;
import ru.practicum.main.dto.response.CategoryDto;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.service.CategoryService;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicCategoryController.class)
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    // ==================== ТЕСТЫ ДЛЯ GET /categories ====================

    @Test
    void getCategories_ShouldReturnCategories_WhenValidParams() throws Exception {
        List<CategoryDto> categories = Arrays.asList(
                new CategoryDto(1L, "Концерты"),
                new CategoryDto(2L, "Выставки"),
                new CategoryDto(3L, "Кино")
        );

        when(categoryService.getCategories(anyInt(), anyInt())).thenReturn(categories);

        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Концерты"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Выставки"))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].name").value("Кино"));
    }

    @Test
    void getCategories_ShouldUseDefaultValues_WhenParamsNotProvided() throws Exception {
        List<CategoryDto> categories = Arrays.asList(
                new CategoryDto(1L, "Концерты")
        );

        when(categoryService.getCategories(0, 10)).thenReturn(categories);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Концерты"));
    }

    @Test
    void getCategories_ShouldReturnBadRequest_WhenFromNegative() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategories_ShouldReturnBadRequest_WhenSizeZero() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategories_ShouldReturnBadRequest_WhenSizeNegative() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategories_ShouldReturnBadRequest_WhenFromIsNotNumber() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("from", "abc")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategories_ShouldReturnBadRequest_WhenSizeIsNotNumber() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "xyz"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategories_ShouldReturnEmptyList_WhenNoCategories() throws Exception {
        when(categoryService.getCategories(anyInt(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== ТЕСТЫ ДЛЯ GET /categories/{catId} ====================

    @Test
    void getCategory_ShouldReturnCategory_WhenExists() throws Exception {
        CategoryDto category = new CategoryDto(1L, "Концерты");

        when(categoryService.getCategory(1L)).thenReturn(category);

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Концерты"));
    }

    @Test
    void getCategory_ShouldReturnNotFound_WhenCategoryDoesNotExist() throws Exception {
        Long nonExistentId = 999L;

        when(categoryService.getCategory(nonExistentId))
                .thenThrow(new NotFoundException("Category with id=" + nonExistentId + " was not found"));

        mockMvc.perform(get("/categories/{catId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category with id=999 was not found"));
    }

    @Test
    void getCategory_ShouldReturnBadRequest_WhenCategoryIdNegative() throws Exception {
        mockMvc.perform(get("/categories/-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategory_ShouldReturnBadRequest_WhenCategoryIdZero() throws Exception {
        mockMvc.perform(get("/categories/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategory_ShouldReturnBadRequest_WhenCategoryIdIsNotNumber() throws Exception {
        mockMvc.perform(get("/categories/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategory_ShouldReturnBadRequest_WhenCategoryIdHasDecimal() throws Exception {
        mockMvc.perform(get("/categories/1.5"))
                .andExpect(status().isBadRequest());
    }
}
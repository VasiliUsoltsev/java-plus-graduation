package ru.practicum.main.categories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.controller.AdminCategoryController;
import ru.practicum.main.dto.request.NewCategoryDto;
import ru.practicum.main.dto.response.CategoryDto;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.service.CategoryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCategoryController.class)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    // ==================== ТЕСТЫ ДЛЯ POST /admin/categories ====================

    @Test
    void addCategory_ShouldReturnCreated_WhenValidRequest() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Концерты");

        CategoryDto response = new CategoryDto(1L, "Концерты");

        when(categoryService.addCategory(any(NewCategoryDto.class))).thenReturn(response);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Концерты"));
    }

    @Test
    void addCategory_ShouldReturnBadRequest_WhenNameIsEmpty() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCategory_ShouldReturnBadRequest_WhenNameIsBlank() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("   ");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCategory_ShouldReturnBadRequest_WhenNameTooLong() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("A".repeat(51));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== ТЕСТЫ ДЛЯ DELETE /admin/categories/{catId} ====================

    @Test
    void deleteCategory_ShouldReturnNoContent_WhenCategoryExists() throws Exception {
        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategory_ShouldReturnNotFound_WhenCategoryDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Category with id=999 was not found"))
                .when(categoryService).deleteCategory(999L);

        mockMvc.perform(delete("/admin/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category with id=999 was not found"));
    }

    @Test
    void deleteCategory_ShouldReturnBadRequest_WhenCategoryIdNegative() throws Exception {
        mockMvc.perform(delete("/admin/categories/-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_ShouldReturnBadRequest_WhenCategoryIdZero() throws Exception {
        mockMvc.perform(delete("/admin/categories/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_ShouldReturnBadRequest_WhenCategoryIdIsNotNumber() throws Exception {
        mockMvc.perform(delete("/admin/categories/abc"))
                .andExpect(status().isBadRequest());
    }

    // ==================== ТЕСТЫ ДЛЯ PATCH /admin/categories/{catId} ====================

    @Test
    void updateCategory_ShouldReturnOk_WhenValidRequest() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Обновленные концерты");

        CategoryDto response = new CategoryDto(1L, "Обновленные концерты");

        when(categoryService.updateCategory(anyLong(), any(CategoryDto.class))).thenReturn(response);

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Обновленные концерты"));
    }

    @Test
    void updateCategory_ShouldReturnBadRequest_WhenCategoryIdNegative() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Обновленные концерты");

        mockMvc.perform(patch("/admin/categories/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_ShouldReturnBadRequest_WhenCategoryIdZero() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Обновленные концерты");

        mockMvc.perform(patch("/admin/categories/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_ShouldReturnBadRequest_WhenCategoryIdIsNotNumber() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Обновленные концерты");

        mockMvc.perform(patch("/admin/categories/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
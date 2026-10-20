package ru.practicum.main.categories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.main.model.Category;
import ru.practicum.main.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category1;
    private Category category2;
    private Category category3;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();

        category1 = Category.builder()
                .name("Концерты")
                .build();

        category2 = Category.builder()
                .name("Выставки")
                .build();

        category3 = Category.builder()
                .name("Кино")
                .build();

        categoryRepository.save(category1);
        categoryRepository.save(category2);
        categoryRepository.save(category3);
    }

    // ==================== ТЕСТЫ ДЛЯ БАЗОВЫХ МЕТОДОВ ====================

    @Test
    void save_ShouldSaveCategory_WhenValid() {
        Category newCategory = Category.builder()
                .name("Спектакли")
                .build();

        Category saved = categoryRepository.save(newCategory);

        assertNotNull(saved.getId());
        assertEquals("Спектакли", saved.getName());
    }

    @Test
    void findById_ShouldReturnCategory_WhenExists() {
        Optional<Category> found = categoryRepository.findById(category1.getId());

        assertTrue(found.isPresent());
        assertEquals(category1.getName(), found.get().getName());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        Optional<Category> found = categoryRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void findAll_ShouldReturnAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        assertThat(categories).hasSize(3);
        assertThat(categories).extracting(Category::getName)
                .containsExactlyInAnyOrder("Концерты", "Выставки", "Кино");
    }

    @Test
    void findAll_ShouldReturnPage_WhenPageableProvided() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Category> page = categoryRepository.findAll(pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void existsById_ShouldReturnTrue_WhenExists() {
        boolean exists = categoryRepository.existsById(category1.getId());

        assertTrue(exists);
    }

    @Test
    void existsById_ShouldReturnFalse_WhenNotExists() {
        boolean exists = categoryRepository.existsById(999L);

        assertFalse(exists);
    }

    @Test
    void deleteById_ShouldDeleteCategory_WhenExists() {
        categoryRepository.deleteById(category1.getId());

        Optional<Category> found = categoryRepository.findById(category1.getId());
        assertFalse(found.isPresent());
        assertThat(categoryRepository.findAll()).hasSize(2);
    }

    @Test
    void delete_ShouldDeleteCategory_WhenPassedEntity() {
        categoryRepository.delete(category1);

        Optional<Category> found = categoryRepository.findById(category1.getId());
        assertFalse(found.isPresent());
        assertThat(categoryRepository.findAll()).hasSize(2);
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        long count = categoryRepository.count();

        assertEquals(3, count);
    }
}

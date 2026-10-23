package ru.practicum.main.users;

import ru.practicum.main.model.User;
import ru.practicum.main.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user1 = new User();
        user1.setName("John Doe");
        user1.setEmail("john@example.com");

        user2 = new User();
        user2.setName("Jane Smith");
        user2.setEmail("jane@example.com");

        user3 = new User();
        user3.setName("Bob Johnson");
        user3.setEmail("bob@example.com");

        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);
        user3 = userRepository.save(user3);
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        boolean exists = userRepository.existsByEmail("john@example.com");
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");
        assertThat(exists).isFalse();
    }

    @Test
    void findByIds_ShouldReturnUsers_WhenIdsProvided() {
        List<Long> ids = Arrays.asList(user1.getId(), user3.getId());
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> usersPage = userRepository.findByIds(ids, pageable);
        List<User> users = usersPage.getContent();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).getId()).isEqualTo(user1.getId());
        assertThat(users.get(0).getName()).isEqualTo("John Doe");
        assertThat(users.get(1).getId()).isEqualTo(user3.getId());
        assertThat(users.get(1).getName()).isEqualTo("Bob Johnson");
    }

    @Test
    void findByIds_ShouldReturnEmptyList_WhenNoUsersFound() {
        List<Long> ids = Arrays.asList(999L, 1000L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> usersPage = userRepository.findByIds(ids, pageable);
        List<User> users = usersPage.getContent();

        assertThat(users).isEmpty();
    }

    @Test
    void findByIds_ShouldRespectPagination() {
        List<Long> ids = Arrays.asList(user1.getId(), user2.getId(), user3.getId());
        Pageable pageable = PageRequest.of(0, 2);
        Page<User> usersPage = userRepository.findByIds(ids, pageable);
        List<User> users = usersPage.getContent();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).getId()).isEqualTo(user1.getId());
        assertThat(users.get(1).getId()).isEqualTo(user2.getId());

        // Проверяем вторую страницу
        pageable = PageRequest.of(1, 2);
        usersPage = userRepository.findByIds(ids, pageable);
        users = usersPage.getContent();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getId()).isEqualTo(user3.getId());
    }

    @Test
    void findAllOrdered_ShouldReturnAllUsersOrderedById() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> usersPage = userRepository.findAllOrdered(pageable);
        List<User> users = usersPage.getContent();

        assertThat(users).hasSize(3);
        assertThat(users.get(0).getId()).isEqualTo(user1.getId());
        assertThat(users.get(0).getName()).isEqualTo("John Doe");
        assertThat(users.get(1).getId()).isEqualTo(user2.getId());
        assertThat(users.get(1).getName()).isEqualTo("Jane Smith");
        assertThat(users.get(2).getId()).isEqualTo(user3.getId());
        assertThat(users.get(2).getName()).isEqualTo("Bob Johnson");
    }

    @Test
    void findAllOrdered_ShouldRespectPagination() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<User> usersPage = userRepository.findAllOrdered(pageable);
        List<User> users = usersPage.getContent();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).getId()).isEqualTo(user1.getId());
        assertThat(users.get(1).getId()).isEqualTo(user2.getId());

        // Проверяем вторую страницу
        pageable = PageRequest.of(1, 2);
        usersPage = userRepository.findAllOrdered(pageable);
        users = usersPage.getContent();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getId()).isEqualTo(user3.getId());
    }

    @Test
    void findAllOrdered_ShouldReturnEmptyPage_WhenOffsetOutOfBounds() {
        Pageable pageable = PageRequest.of(10, 20);
        Page<User> usersPage = userRepository.findAllOrdered(pageable);
        List<User> users = usersPage.getContent();

        assertThat(users).isEmpty();
    }
}
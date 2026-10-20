package ru.practicum.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.User;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.id IN :ids ORDER BY u.id")
    Page<User> findByIds(@Param("ids") List<Long> ids, Pageable pageable);

    @Query("SELECT u FROM User u ORDER BY u.id")
    Page<User> findAllOrdered(Pageable pageable);
}
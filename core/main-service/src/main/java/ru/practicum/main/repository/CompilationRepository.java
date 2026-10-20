package ru.practicum.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.Compilation;

import java.util.Optional;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    @EntityGraph(attributePaths = {"events"})
    Page<Compilation> findByPinned(Boolean pinned, Pageable pageable);

    @EntityGraph(attributePaths = {"events"})
    Page<Compilation> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"events"})
    Optional<Compilation> findById(Long id);

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);
}
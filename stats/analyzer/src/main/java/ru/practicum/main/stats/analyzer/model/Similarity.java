package ru.practicum.main.stats.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "similarities", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"eventA", "eventB"})
})
@IdClass(Similarity.SimilarityId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Similarity {
    @Id
    @Column(name = "eventA", nullable = false)
    private Long eventA;

    @Id
    @Column(name = "eventB", nullable = false)
    private Long eventB;

    @Column(name = "similarity", nullable = false)
    private double score;

    @Column(name = "ts", nullable = false)
    private Instant timestamp;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SimilarityId implements Serializable {
        private Long eventA;
        private Long eventB;
    }
}
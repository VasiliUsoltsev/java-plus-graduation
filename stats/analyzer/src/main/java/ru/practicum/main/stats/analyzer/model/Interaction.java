package ru.practicum.main.stats.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "interactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "event_id"})
})
@IdClass(Interaction.InteractionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interaction {
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "rating", nullable = false)
    private double rating;

    @Column(name = "ts", nullable = false)
    private Instant timestamp;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class InteractionId implements Serializable {
        private Long userId;
        private Long eventId;
    }
}

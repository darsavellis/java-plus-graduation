package ru.practicum.ewm.stats.analyzer.dal.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event_similarities")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "event_a")
    Long eventA;
    @Column(name = "event_b")
    Long eventB;
    @Column(name = "score")
    double score;
    @Column(name = "timestamp")
    Instant timestamp;
}

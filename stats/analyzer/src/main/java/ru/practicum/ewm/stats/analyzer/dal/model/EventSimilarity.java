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
    long id;
    @Column(name = "event_a")
    long eventA;
    @Column(name = "event_b")
    long eventB;
    @Column(name = "score")
    double score;
    @Column(name = "timestamp")
    Instant timestamp;
}

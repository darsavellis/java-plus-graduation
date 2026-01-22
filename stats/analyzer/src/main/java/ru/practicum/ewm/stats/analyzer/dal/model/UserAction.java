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
@Table(name = "user_actions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "event_id"})
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    @Column(name = "user_id")
    long userId;
    @Column(name = "event_id")
    long eventId;
    @Column(name = "weight")
    double weight;
    @Column(name = "timestamp")
    Instant timestamp;
}

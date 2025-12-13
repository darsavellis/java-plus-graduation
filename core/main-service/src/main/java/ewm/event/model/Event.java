package ewm.event.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@Table(name = "events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "annotation", length = 2000, nullable = false)
    String annotation;
    @Column(name = "category_id")
    Long categoryId;
    @Column(name = "created_on")
    LocalDateTime createdOn = LocalDateTime.now();
    @Column(name = "description", length = 7000)
    String description;
    @Column(name = "event_date", nullable = false)
    LocalDateTime eventDate;
    @Column(name = "initiator_id")
    Long initiatorId;
    @Embedded
    Location location;
    @Column(name = "paid", nullable = false)
    boolean paid;
    @Column(name = "participant_limit")
    int participantLimit;
    @Column(name = "published_on")
    LocalDateTime publishedOn;
    @Column(name = "request_moderation")
    boolean requestModeration;
    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 50)
    EventState state = EventState.PENDING;
    @Column(name = "title", length = 120, nullable = false)
    String title;
}

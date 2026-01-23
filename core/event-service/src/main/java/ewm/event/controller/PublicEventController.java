package ewm.event.controller;

import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventShortDto;
import ewm.event.dto.PublicEventParam;
import ewm.event.service.PublicEventService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicEventController {
    final PublicEventService publicEventService;

    @GetMapping
    List<EventShortDto> getAllBy(@Valid @ModelAttribute PublicEventParam publicEventParam,
                                 @RequestParam(defaultValue = "0") int from,
                                 @RequestParam(defaultValue = "10") int size) {
        return publicEventService.getAllBy(publicEventParam, PageRequest.of(from, size));
    }

    @GetMapping("/{eventId}")
    EventFullDto getBy(@PathVariable long eventId, @RequestHeader("X-EWM-USER-ID") long userId) {
        return publicEventService.getBy(eventId, userId);
    }

    @GetMapping("/internal/{eventId}")
    EventFullDto getByInternal(@PathVariable long eventId) {
        return publicEventService.getBy(eventId);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendationsForUser(@RequestHeader("X-EWM-USER-ID") long userId) {
        return publicEventService.getRecommendations(userId);
    }

    @PutMapping("/{eventId}/like")
    public void like(@RequestHeader("X-EWM-USER-ID") long userId, @PathVariable long eventId) {
        publicEventService.like(userId, eventId);
    }
}

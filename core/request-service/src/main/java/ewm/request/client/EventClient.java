package ewm.request.client;


import ewm.request.dto.EventFullDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service", path = "/events", fallback = EventClientFallback.class)
public interface EventClient {
    @GetMapping("/internal/{eventId}")
    EventFullDto getByInternal(@PathVariable("eventId") long eventId);
}

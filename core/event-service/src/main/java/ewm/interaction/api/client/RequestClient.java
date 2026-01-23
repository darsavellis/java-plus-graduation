package ewm.interaction.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", fallback = RequestClientFallback.class)
public interface RequestClient {
    @GetMapping("/users/{userId}/requests/confirmed")
    Map<Long, Long> getConfirmedRequestsMap(@PathVariable long userId, @RequestParam List<Long> eventIds);

    @GetMapping("/admin/requests/confirmed")
    Map<Long, Long> getConfirmedRequestsMap(@RequestParam List<Long> eventIds);

    @GetMapping("/users/{userId}/requests/participated/{eventId}")
    boolean hasUserParticipated(@PathVariable long userId, @PathVariable long eventId);
}

package ewm.request.client;

import ewm.request.dto.EventFullDto;
import ewm.request.exception.ConflictException;
import org.springframework.stereotype.Component;

@Component
public class EventClientFallback implements EventClient {
    @Override
    public EventFullDto getByInternal(long eventId) {
        throw new ConflictException("Failed to get event");
    }
}

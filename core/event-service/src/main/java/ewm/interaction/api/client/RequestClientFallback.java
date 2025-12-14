package ewm.interaction.api.client;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RequestClientFallback implements RequestClient {
    @Override
    public Map<Long, Long> getConfirmedRequestsMap(long userId, List<Long> eventIds) {
        return Map.of();
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        return Map.of();
    }
}
